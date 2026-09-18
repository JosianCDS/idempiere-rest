package com.trekglobal.idempiere.rest.api.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSysConfig;
import org.compiere.util.DB;

import com.trekglobal.idempiere.rest.api.v1.auth.LoginParameters;

/** Persistent, opaque and single-use REST pre-authentication challenge. */
public class MAuthChallenge {
	public static final String TABLE_NAME = "REST_AuthChallenge";
	public static final String MFA_EXPIRATION_SYSCONFIG = "REST_MFA_CHALLENGE_EXPIRATION_SECONDS";
	public static final String CONTEXT_EXPIRATION_SYSCONFIG = "REST_CONTEXT_CHALLENGE_EXPIRATION_SECONDS";
	public static final String MAX_ATTEMPTS_SYSCONFIG = "REST_MFA_MAX_ATTEMPTS";
	public static final String STATE_CLIENT_REQUIRED = "CLIENT_REQUIRED";
	public static final String STATE_MFA_REQUIRED = "MFA_REQUIRED";
	public static final String STATE_CONTEXT_REQUIRED = "CONTEXT_REQUIRED";

	private String tokenHash;
	private int userId;
	private String userName;
	private String clients;
	private String clientId;
	private String roleId;
	private String organizationId;
	private String warehouseId;
	private String language;
	private String state;
	private Timestamp expiresAt;
	private int attemptCount;
	private boolean active;
	private Timestamp consumedAt;

	public static final class CreatedChallenge {
		private final String token;
		private final MAuthChallenge challenge;
		private CreatedChallenge(String token, MAuthChallenge challenge) { this.token = token; this.challenge = challenge; }
		public String getToken() { return token; }
		public MAuthChallenge getChallenge() { return challenge; }
	}

	public static CreatedChallenge create(int userId, String userName, String clients) {
		return insert(userId, userName, clients, null, STATE_CLIENT_REQUIRED,
				Math.max(30, MSysConfig.getIntValue(MFA_EXPIRATION_SYSCONFIG, 300)));
	}

	private static CreatedChallenge insert(int userId, String userName, String clients, LoginParameters parameters,
			String state, int ttl) {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String hash = hash(token);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Timestamp expires = new Timestamp(now.getTime() + ttl * 1000L);
		String sql = "INSERT INTO REST_AuthChallenge (REST_AuthChallenge_UU,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,TokenHash,AD_User_ID,UserName,AllowedClients,ClientParameter,RoleParameter,OrgParameter,WarehouseParameter,Language,ChallengeState,ExpiresAt,AttemptCount) VALUES (?,0,0,'Y',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)";
		DB.executeUpdateEx(sql, new Object[] { UUID.randomUUID().toString(), now, userId, now, userId, hash, userId,
				userName, clients, value(parameters, 0), value(parameters, 1), value(parameters, 2), value(parameters, 3),
				value(parameters, 4), state, expires }, null);
		return new CreatedChallenge(token, get(token));
	}

	private static String value(LoginParameters p, int field) {
		if (p == null) return null;
		switch (field) {
		case 0: return p.getClientId();
		case 1: return p.getRoleId();
		case 2: return p.getOrganizationId();
		case 3: return p.getWarehouseId();
		default: return p.getLanguage();
		}
	}

	public static MAuthChallenge get(String token) {
		if (token == null || token.isBlank()) return null;
		String sql = "SELECT TokenHash,AD_User_ID,UserName,AllowedClients,ClientParameter,RoleParameter,OrgParameter,WarehouseParameter,Language,ChallengeState,ExpiresAt,AttemptCount,IsActive,ConsumedAt FROM REST_AuthChallenge WHERE TokenHash=?";
		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setString(1, hash(token));
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) return null;
				MAuthChallenge c = new MAuthChallenge();
				c.tokenHash=rs.getString(1); c.userId=rs.getInt(2); c.userName=rs.getString(3); c.clients=rs.getString(4);
				c.clientId=rs.getString(5); c.roleId=rs.getString(6); c.organizationId=rs.getString(7);
				c.warehouseId=rs.getString(8); c.language=rs.getString(9); c.state=rs.getString(10); c.expiresAt=rs.getTimestamp(11);
				c.attemptCount=rs.getInt(12); c.active="Y".equals(rs.getString(13)); c.consumedAt=rs.getTimestamp(14);
				return c;
			}
		} catch (SQLException e) { throw new AdempiereException("Unable to read REST authentication challenge", e); }
	}

	public boolean isUsable() {
		return active && consumedAt == null && expiresAt != null && expiresAt.after(new Timestamp(System.currentTimeMillis()));
	}

	public boolean hasState(String expected) { return expected.equals(state); }

	public void setParameters(LoginParameters p) {
		String sql = "UPDATE REST_AuthChallenge SET ClientParameter=?,RoleParameter=?,OrgParameter=?,WarehouseParameter=?,Language=?,Updated=getDate(),UpdatedBy=? WHERE TokenHash=? AND IsActive='Y' AND ConsumedAt IS NULL AND ExpiresAt>getDate()";
		int count = DB.executeUpdateEx(sql, new Object[] { p.getClientId(), p.getRoleId(), p.getOrganizationId(), p.getWarehouseId(), p.getLanguage(), userId, tokenHash }, null);
		if (count != 1) throw new AdempiereException("Authentication challenge is no longer valid");
		clientId=p.getClientId(); roleId=p.getRoleId(); organizationId=p.getOrganizationId(); warehouseId=p.getWarehouseId(); language=p.getLanguage();
	}

	public void markMFARequired(LoginParameters p) {
		setParameters(p);
		String sql = "UPDATE REST_AuthChallenge SET ChallengeState=?,Updated=getDate(),UpdatedBy=? WHERE TokenHash=? AND ChallengeState=? AND IsActive='Y' AND ConsumedAt IS NULL AND ExpiresAt>getDate()";
		int count = DB.executeUpdateEx(sql, new Object[] { STATE_MFA_REQUIRED, userId, tokenHash, STATE_CLIENT_REQUIRED }, null);
		if (count != 1) throw new AdempiereException("Authentication challenge is no longer valid");
		state = STATE_MFA_REQUIRED;
	}

	public CreatedChallenge rotateToContext(int registrationId) {
		if (!consume(state)) return null;
		int client = parseId(clientId);
		int ttl = Math.max(60, MSysConfig.getIntValue(CONTEXT_EXPIRATION_SYSCONFIG, 900, client));
		CreatedChallenge created = insert(userId, userName, clients, getParameters(), STATE_CONTEXT_REQUIRED, ttl);
		if (registrationId > 0)
			DB.executeUpdateEx("UPDATE REST_AuthChallenge SET MFAVerifiedAt=getDate(),MFA_Registration_ID=? WHERE TokenHash=?",
					new Object[] { registrationId, created.challenge.tokenHash }, null);
		return created;
	}

	public boolean renewContext() {
		if (!hasState(STATE_CONTEXT_REQUIRED)) return false;
		int client = parseId(clientId);
		int ttl = Math.max(60, MSysConfig.getIntValue(CONTEXT_EXPIRATION_SYSCONFIG, 900, client));
		Timestamp expires = new Timestamp(System.currentTimeMillis() + ttl * 1000L);
		String sql = "UPDATE REST_AuthChallenge SET ExpiresAt=?,Updated=getDate(),UpdatedBy=? WHERE TokenHash=? AND ChallengeState=? AND IsActive='Y' AND ConsumedAt IS NULL AND ExpiresAt>getDate()";
		int count = DB.executeUpdateEx(sql, new Object[] { expires, userId, tokenHash, STATE_CONTEXT_REQUIRED }, null);
		if (count == 1) expiresAt = expires;
		return count == 1;
	}

	public boolean consume() { return consume(state); }

	public boolean consume(String requiredState) {
		String sql = "UPDATE REST_AuthChallenge SET IsActive='N',ConsumedAt=getDate(),Updated=getDate(),UpdatedBy=? WHERE TokenHash=? AND ChallengeState=? AND IsActive='Y' AND ConsumedAt IS NULL AND ExpiresAt>getDate()";
		return DB.executeUpdateEx(sql, new Object[] { userId, tokenHash, requiredState }, null) == 1;
	}

	public int recordFailure() {
		int client = parseId(clientId);
		int max = Math.max(1, MSysConfig.getIntValue(MAX_ATTEMPTS_SYSCONFIG, 5, client));
		String sql = "UPDATE REST_AuthChallenge SET AttemptCount=AttemptCount+1,IsActive=CASE WHEN AttemptCount+1>=? THEN 'N' ELSE IsActive END,Updated=getDate(),UpdatedBy=? WHERE TokenHash=? AND ChallengeState=? AND IsActive='Y' AND ConsumedAt IS NULL";
		DB.executeUpdateEx(sql, new Object[] { max, userId, tokenHash, STATE_MFA_REQUIRED }, null);
		attemptCount++;
		if (attemptCount >= max) active=false;
		return attemptCount;
	}

	public LoginParameters getParameters() {
		LoginParameters p = new LoginParameters();
		p.setClientId(clientId); p.setRoleId(roleId); p.setOrganizationId(organizationId); p.setWarehouseId(warehouseId); p.setLanguage(language);
		return p;
	}

	public boolean allowsClient(int id) {
		if (clients == null) return false;
		for (String value : clients.split(",")) if (Integer.toString(id).equals(value.trim())) return true;
		return false;
	}

	private static int parseId(String value) {
		try { return value == null ? 0 : Integer.parseInt(value); } catch (NumberFormatException e) { return 0; }
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			StringBuilder value = new StringBuilder(64);
			for (byte b : digest) value.append(String.format("%02x", b));
			return value.toString();
		} catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
	}

	public int getUserId() { return userId; }
	public String getUserName() { return userName; }
	public String getClients() { return clients; }
	public String getState() { return state; }
	public Timestamp getExpiresAt() { return expiresAt; }
}
