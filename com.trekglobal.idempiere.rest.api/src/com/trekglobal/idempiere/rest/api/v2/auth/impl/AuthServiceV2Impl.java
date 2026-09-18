package com.trekglobal.idempiere.rest.api.v2.auth.impl;

import java.sql.Timestamp;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.LogAuthFailure;
import org.compiere.model.IMFAMechanism;
import org.compiere.model.MClient;
import org.compiere.model.MMFAMethod;
import org.compiere.model.MMFARegistration;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Login;
import org.compiere.util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.model.MAuthChallenge;
import com.trekglobal.idempiere.rest.api.model.MAuthChallenge.CreatedChallenge;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginParameters;
import com.trekglobal.idempiere.rest.api.v1.auth.filter.RequestFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;
import com.trekglobal.idempiere.rest.api.v2.auth.AuthRequestV2;
import com.trekglobal.idempiere.rest.api.v2.auth.AuthServiceV2;
import com.trekglobal.idempiere.rest.api.v2.auth.ContextRequestV2;
import com.trekglobal.idempiere.rest.api.v2.auth.MFARegistrationCompleteRequest;
import com.trekglobal.idempiere.rest.api.v2.auth.MFARegistrationRequest;
import com.trekglobal.idempiere.rest.api.v2.auth.MFAVerificationRequest;

/** MFA-aware progressive authentication and TOTP self-service for iDempiere 12. */
public class AuthServiceV2Impl implements AuthServiceV2 {
	private LogAuthFailure logAuthFailure;
	private @Context HttpServletRequest servletRequest;

	@Override
	public Response authenticate(AuthRequestV2 request) {
		if (request == null || Util.isEmpty(request.getUserName(), true) || Util.isEmpty(request.getPassword(), true))
			return error(Status.BAD_REQUEST, "invalid_request", "Username and password are required");
		Login login = new Login(Env.getCtx());
		KeyNamePair[] clients = login.getClients(request.getUserName(), request.getPassword(), AuthServiceImpl.ROLE_TYPES_WEBSERVICE);
		if (clients == null || clients.length == 0) {
			logFailure(request.getUserName(), login.getLoginErrMsg());
			return error(Status.UNAUTHORIZED, "invalid_credentials", "Invalid username or password");
		}
		MUser user = MUser.get(Env.getCtx(), request.getUserName());
		StringBuilder allowed = new StringBuilder();
		JsonArray clientNodes = new JsonArray();
		for (KeyNamePair client : clients) {
			if (allowed.length() > 0) allowed.append(',');
			allowed.append(client.getKey());
			JsonObject node = new JsonObject(); node.addProperty("id", client.getKey()); node.addProperty("name", client.getName()); clientNodes.add(node);
		}
		CreatedChallenge created = MAuthChallenge.create(user.getAD_User_ID(), request.getUserName(), allowed.toString());
		if (request.getParameters() != null) {
			ContextRequestV2 context = new ContextRequestV2();
			context.setChallengeToken(created.getToken()); context.setParameters(request.getParameters());
			return selectContext(context);
		}
		JsonObject json = challengeResponse("CLIENT_REQUIRED", created);
		json.add("clients", clientNodes);
		return Response.ok(json.toString()).build();
	}

	@Override
	public Response selectContext(ContextRequestV2 request) {
		if (request == null || request.getParameters() == null || Util.isEmpty(request.getChallengeToken(), true))
			return error(Status.BAD_REQUEST, "invalid_request", "Challenge token and login parameters are required");
		MAuthChallenge challenge = MAuthChallenge.get(request.getChallengeToken());
		if (challenge == null || !challenge.isUsable()) return invalidChallenge();
		LoginParameters parameters = request.getParameters();
		if (challenge.hasState(MAuthChallenge.STATE_CONTEXT_REQUIRED))
			return finishContext(challenge, parameters);
		if (!challenge.hasState(MAuthChallenge.STATE_CLIENT_REQUIRED)) return invalidChallenge();
		int clientId = resolveClientId(parameters.getClientId());
		if (clientId < 0 || !challenge.allowsClient(clientId))
			return error(Status.UNAUTHORIZED, "invalid_context", "Invalid clientId");
		parameters.setClientId(Integer.toString(clientId));
		setIdentityContext(challenge, clientId);
		List<MMFARegistration> registrations = validRegistrations(challenge.getUserId(), clientId);
		JsonArray totp = totpRegistrations(registrations);
		if (!registrations.isEmpty() && totp.size() == 0)
			return error(Status.FORBIDDEN, "unsupported_mfa_method", "The configured MFA method is not supported by API v2");
		if (totp.size() > 0) {
			challenge.markMFARequired(parameters);
			JsonObject json = new JsonObject(); json.addProperty("authenticationState", "MFA_REQUIRED");
			json.addProperty("mfaRequired", true); json.addProperty("challengeToken", request.getChallengeToken());
			json.addProperty("expiresIn", secondsRemaining(challenge)); json.add("methods", totp);
			return Response.status(Status.ACCEPTED).entity(json.toString()).build();
		}
		challenge.setParameters(parameters);
		if (hasCompleteContext(parameters)) {
			if (!challenge.consume(MAuthChallenge.STATE_CLIENT_REQUIRED)) return invalidChallenge();
			return issueTokens(challenge);
		}
		return contextChallenge(challenge.rotateToContext(0));
	}

	@Override
	public Response verifyMFA(MFAVerificationRequest request) {
		if (request == null || request.getRegistrationId() <= 0 || Util.isEmpty(request.getCode(), true)
				|| Util.isEmpty(request.getChallengeToken(), true))
			return error(Status.BAD_REQUEST, "invalid_request", "Challenge token, registrationId and code are required");
		MAuthChallenge challenge = MAuthChallenge.get(request.getChallengeToken());
		if (challenge == null || !challenge.isUsable() || !challenge.hasState(MAuthChallenge.STATE_MFA_REQUIRED)) return invalidChallenge();
		int clientId = resolveClientId(challenge.getParameters().getClientId());
		if (clientId < 0 || !challenge.allowsClient(clientId)) return invalidChallenge();
		setIdentityContext(challenge, clientId);
		MMFARegistration registration = new MMFARegistration(Env.getCtx(), request.getRegistrationId(), null);
		if (!isUsableRegistration(registration, challenge.getUserId(), clientId)) {
			challenge.recordFailure(); logFailure(challenge.getUserName(), "Invalid MFA registration");
			return error(Status.UNAUTHORIZED, "invalid_mfa_code", "Invalid MFA code");
		}
		String message = registration.validateCode(registration, request.getCode(), false);
		if (message != null) {
			challenge.recordFailure(); logFailure(challenge.getUserName(), message);
			return error(Status.UNAUTHORIZED, "invalid_mfa_code", "Invalid MFA code");
		}
		Env.setContext(Env.getCtx(), Env.MFA_Registration_ID, registration.getMFA_Registration_ID());
		if (hasCompleteContext(challenge.getParameters())) {
			if (!challenge.consume(MAuthChallenge.STATE_MFA_REQUIRED)) return invalidChallenge();
			return issueTokens(challenge);
		}
		return contextChallenge(challenge.rotateToContext(registration.getMFA_Registration_ID()));
	}

	@Override
	public Response getRoles(String token) {
		MAuthChallenge challenge = context(token);
		if (challenge == null) return invalidChallenge();
		setIdentityContext(challenge, selectedClient(challenge));
		AuthServiceImpl v1 = v1();
		Response response = v1.getRoles(selectedClient(challenge));
		return renewOnSuccess(challenge, response);
	}

	@Override
	public Response getOrganizations(String token, int roleId) {
		MAuthChallenge challenge = context(token);
		if (challenge == null || roleId <= 0) return invalidChallenge();
		setIdentityContext(challenge, selectedClient(challenge));
		Response response = v1().getOrganizations(selectedClient(challenge), roleId);
		if (response.getStatus() == Status.OK.getStatusCode()) {
			LoginParameters p = challenge.getParameters(); p.setRoleId(Integer.toString(roleId)); challenge.setParameters(p);
		}
		return renewOnSuccess(challenge, response);
	}

	@Override
	public Response getWarehouses(String token, int roleId, int organizationId) {
		MAuthChallenge challenge = context(token);
		if (challenge == null || roleId <= 0 || organizationId < 0) return invalidChallenge();
		setIdentityContext(challenge, selectedClient(challenge));
		Response response = v1().getWarehouses(selectedClient(challenge), roleId, organizationId);
		if (response.getStatus() == Status.OK.getStatusCode()) {
			LoginParameters p = challenge.getParameters(); p.setRoleId(Integer.toString(roleId));
			p.setOrganizationId(Integer.toString(organizationId)); challenge.setParameters(p);
		}
		return renewOnSuccess(challenge, response);
	}

	@Override
	public Response getLanguage(String token) {
		MAuthChallenge challenge = context(token);
		if (challenge == null) return invalidChallenge();
		setIdentityContext(challenge, selectedClient(challenge));
		return renewOnSuccess(challenge, v1().getClientLanguage(selectedClient(challenge)));
	}

	@Override
	public Response getMFAMethods() {
		List<MMFAMethod> methods = new Query(Env.getCtx(), MMFAMethod.Table_Name, "Method=?", null)
				.setParameters(MMFAMethod.METHOD_Time_BasedOne_TimePassword).setOnlyActiveRecords(true).setOrderBy("Name").list();
		JsonArray array = new JsonArray();
		for (MMFAMethod method : methods) {
			JsonObject item = new JsonObject(); item.addProperty("methodId", method.getMFA_Method_ID());
			item.addProperty("name", method.getName()); item.addProperty("type", "TOTP"); array.add(item);
		}
		JsonObject json = new JsonObject(); json.add("methods", array); return Response.ok(json.toString()).build();
	}

	@Override
	public Response getMFARegistrations(int requestedUserId) {
		int currentUserId = Env.getAD_User_ID(Env.getCtx());
		int userId = requestedUserId > 0 ? requestedUserId : currentUserId;
		if (userId != currentUserId && !MUser.get(currentUserId).isAdministrator())
			return error(Status.FORBIDDEN, "forbidden", "Administrator access is required");
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		List<MMFARegistration> registrations = new Query(Env.getCtx(), MMFARegistration.Table_Name,
				"AD_User_ID=? AND AD_Client_ID IN (0,?)", null).setParameters(userId, clientId).setOrderBy("Created DESC").list();
		JsonArray array = new JsonArray();
		for (MMFARegistration registration : registrations) array.add(registrationJson(registration));
		JsonObject json = new JsonObject(); json.add("registrations", array); return Response.ok(json.toString()).build();
	}

	@Override
	public Response registerMFA(MFARegistrationRequest request) {
		if (request == null || request.getMethodId() <= 0)
			return error(Status.BAD_REQUEST, "invalid_request", "methodId is required");
		MMFAMethod method = new MMFAMethod(Env.getCtx(), request.getMethodId(), null);
		if (method.get_ID() <= 0 || !method.isActive()
				|| (method.getAD_Client_ID() != 0 && method.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx()))
				|| !MMFAMethod.METHOD_Time_BasedOne_TimePassword.equals(method.getMethod()))
			return error(Status.BAD_REQUEST, "invalid_mfa_method", "An active TOTP method is required");
		try {
			Object[] values = method.getMFAMechanism().register(Env.getCtx(), method, request.getName(), null);
			MMFARegistration registration = null; String qrCode = null; String secret = null;
			for (Object value : values) {
				if (value instanceof MMFARegistration) registration = (MMFARegistration) value;
				else if (value instanceof String && ((String) value).startsWith("data:image/")) qrCode = (String) value;
			}
			if (values.length > 5 && values[5] instanceof String) secret = (String) values[5];
			if (registration == null) throw new AdempiereException("TOTP registration was not created");
			JsonObject json = registrationJson(registration); json.addProperty("qrCode", qrCode); json.addProperty("secret", secret);
			return Response.status(Status.CREATED).entity(json.toString()).build();
		} catch (RuntimeException e) {
			return error(Status.BAD_REQUEST, "mfa_registration_failed", e.getLocalizedMessage());
		}
	}

	@Override
	public Response completeMFA(int registrationId, MFARegistrationCompleteRequest request) {
		if (request == null || Util.isEmpty(request.getCode(), true))
			return error(Status.BAD_REQUEST, "invalid_request", "code is required");
		MMFARegistration registration = ownRegistration(registrationId, false);
		if (registration == null) return error(Status.NOT_FOUND, "registration_not_found", "MFA registration was not found");
		if (!registration.isActive()) return error(Status.GONE, "registration_inactive", "MFA registration is inactive");
		if (registration.isValid()) return error(Status.CONFLICT, "registration_already_valid", "MFA registration is already valid");
		if (registration.getExpiration() != null && registration.getExpiration().before(new Timestamp(System.currentTimeMillis())))
			return error(Status.GONE, "registration_expired", "MFA registration has expired");
		try {
			MMFAMethod method = new MMFAMethod(Env.getCtx(), registration.getMFA_Method_ID(), null);
			String message = method.getMFAMechanism().complete(Env.getCtx(), registration, request.getCode(), request.getName(), request.isPreferred(), null);
			JsonObject json = registrationJson(registration); json.addProperty("message", message); return Response.ok(json.toString()).build();
		} catch (RuntimeException e) {
			return error(Status.UNAUTHORIZED, "invalid_mfa_code", e.getLocalizedMessage());
		}
	}

	@Override
	public Response revokeMFA(int registrationId) {
		MMFARegistration registration = ownRegistration(registrationId, true);
		if (registration == null) return error(Status.NOT_FOUND, "registration_not_found", "MFA registration was not found");
		registration.setIsActive(false); registration.setMFAUnregisteredAt(new Timestamp(System.currentTimeMillis())); registration.saveCrossTenantSafeEx();
		return Response.noContent().build();
	}

	private Response finishContext(MAuthChallenge challenge, LoginParameters parameters) {
		int selectedClient = selectedClient(challenge);
		int requestedClient = resolveClientId(parameters.getClientId());
		if (requestedClient < 0) parameters.setClientId(Integer.toString(selectedClient));
		else if (requestedClient != selectedClient) return error(Status.UNAUTHORIZED, "invalid_context", "The client cannot be changed");
		challenge.setParameters(parameters);
		if (!challenge.consume(MAuthChallenge.STATE_CONTEXT_REQUIRED)) return invalidChallenge();
		return issueTokens(challenge);
	}

	private Response contextChallenge(CreatedChallenge created) {
		if (created == null) return invalidChallenge();
		return Response.ok(challengeResponse("CONTEXT_REQUIRED", created).toString()).build();
	}

	private JsonObject challengeResponse(String state, CreatedChallenge created) {
		JsonObject json = new JsonObject(); json.addProperty("authenticationState", state);
		json.addProperty("challengeToken", created.getToken()); json.addProperty("expiresIn", secondsRemaining(created.getChallenge())); return json;
	}

	private Response renewOnSuccess(MAuthChallenge challenge, Response response) {
		if (response.getStatus() == Status.OK.getStatusCode() && !challenge.renewContext()) return invalidChallenge();
		return response;
	}

	private MAuthChallenge context(String token) {
		MAuthChallenge challenge = MAuthChallenge.get(token);
		return challenge != null && challenge.isUsable() && challenge.hasState(MAuthChallenge.STATE_CONTEXT_REQUIRED) ? challenge : null;
	}

	private Response issueTokens(MAuthChallenge challenge) {
		setIdentityContext(challenge, resolveClientId(challenge.getParameters().getClientId()));
		return v1().changeLoginParameters(challenge.getParameters());
	}

	private AuthServiceImpl v1() { AuthServiceImpl service = new AuthServiceImpl(); service.setRequest(servletRequest); return service; }

	private void setIdentityContext(MAuthChallenge challenge, int clientId) {
		Env.setContext(Env.getCtx(), Env.AD_USER_ID, challenge.getUserId()); Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, clientId);
		Env.setContext(Env.getCtx(), RequestFilter.LOGIN_NAME, challenge.getUserName()); Env.setContext(Env.getCtx(), RequestFilter.LOGIN_CLIENTS, challenge.getClients());
	}

	private List<MMFARegistration> validRegistrations(int userId, int clientId) {
		return new Query(Env.getCtx(), MMFARegistration.Table_Name, "IsValid='Y' AND AD_User_ID=? AND AD_Client_ID IN (0,?)", null)
				.setParameters(userId, clientId).setOnlyActiveRecords(true).setOrderBy("IsUserMFAPreferred DESC, Name").list();
	}

	private JsonArray totpRegistrations(List<MMFARegistration> registrations) {
		JsonArray result = new JsonArray();
		for (MMFARegistration registration : registrations) if (isTotp(registration)) result.add(registrationJson(registration));
		return result;
	}

	private JsonObject registrationJson(MMFARegistration registration) {
		JsonObject item = new JsonObject(); item.addProperty("registrationId", registration.getMFA_Registration_ID());
		item.addProperty("userId", registration.getAD_User_ID()); item.addProperty("name", registration.getName()); item.addProperty("type", "TOTP");
		item.addProperty("active", registration.isActive()); item.addProperty("valid", registration.isValid());
		item.addProperty("preferred", registration.isUserMFAPreferred());
		if (registration.getExpiration() != null) item.addProperty("expiresAt", registration.getExpiration().toInstant().toString());
		return item;
	}

	private boolean isUsableRegistration(MMFARegistration registration, int userId, int clientId) {
		return registration.get_ID() > 0 && registration.isActive() && registration.isValid() && registration.getAD_User_ID() == userId
				&& (registration.getAD_Client_ID() == 0 || registration.getAD_Client_ID() == clientId) && isTotp(registration);
	}

	private boolean isTotp(MMFARegistration registration) {
		MMFAMethod method = new MMFAMethod(Env.getCtx(), registration.getMFA_Method_ID(), null);
		return MMFAMethod.METHOD_Time_BasedOne_TimePassword.equals(method.getMethod());
	}

	private MMFARegistration ownRegistration(int registrationId, boolean allowAdministrator) {
		MMFARegistration registration = new MMFARegistration(Env.getCtx(), registrationId, null);
		if (registration.get_ID() <= 0 || (registration.getAD_Client_ID() != 0 && registration.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx()))) return null;
		int currentUserId = Env.getAD_User_ID(Env.getCtx());
		if (registration.getAD_User_ID() != currentUserId && (!allowAdministrator || !MUser.get(currentUserId).isAdministrator())) return null;
		return registration;
	}

	private int selectedClient(MAuthChallenge challenge) { return resolveClientId(challenge.getParameters().getClientId()); }
	private boolean hasCompleteContext(LoginParameters p) { return !Util.isEmpty(p.getRoleId(), true) && !Util.isEmpty(p.getOrganizationId(), true); }
	private int resolveClientId(String value) {
		if (Util.isEmpty(value, true)) return -1;
		if (Pattern.matches("\\d+", value.trim())) return Integer.parseInt(value.trim());
		return new Query(Env.getCtx(), MClient.Table_Name, "IsActive='Y' AND Value=?", null).setParameters(value.trim()).firstId();
	}
	private long secondsRemaining(MAuthChallenge challenge) { return Math.max(0, (challenge.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000L); }
	private Response invalidChallenge() { return error(Status.UNAUTHORIZED, "invalid_challenge", "Authentication challenge is invalid or expired"); }
	private Response error(Status status, String type, String detail) {
		return Response.status(status).entity(new ErrorBuilder().status(status).type(type).title("Authentication error").append(detail).build().toString()).build();
	}
	private void logFailure(String userName, String detail) {
		String ip = servletRequest == null ? null : servletRequest.getHeader("X-Forwarded-For");
		if (ip == null && servletRequest != null) ip = servletRequest.getRemoteAddr();
		if (logAuthFailure == null) logAuthFailure = new LogAuthFailure();
		logAuthFailure.log(ip, "/api/v2", userName, detail == null ? "Authentication failed" : detail);
	}
}
