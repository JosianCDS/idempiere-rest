package com.trekglobal.idempiere.rest.api.v2.auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("v2/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AuthServiceV2 {
	@POST @Path("tokens") Response authenticate(AuthRequestV2 request);
	@PUT @Path("tokens") Response selectContext(ContextRequestV2 request);
	@POST @Path("mfa/verify") Response verifyMFA(MFAVerificationRequest request);
	@GET @Path("roles") Response getRoles(@HeaderParam("X-Auth-Challenge") String challengeToken);
	@GET @Path("organizations") Response getOrganizations(@HeaderParam("X-Auth-Challenge") String challengeToken, @QueryParam("role") int roleId);
	@GET @Path("warehouses") Response getWarehouses(@HeaderParam("X-Auth-Challenge") String challengeToken, @QueryParam("role") int roleId, @QueryParam("organization") int organizationId);
	@GET @Path("language") Response getLanguage(@HeaderParam("X-Auth-Challenge") String challengeToken);
	@GET @Path("mfa/methods") Response getMFAMethods();
	@GET @Path("mfa/registrations") Response getMFARegistrations(@QueryParam("userId") int userId);
	@POST @Path("mfa/registrations") Response registerMFA(MFARegistrationRequest request);
	@POST @Path("mfa/registrations/{id}/complete") Response completeMFA(@PathParam("id") int registrationId, MFARegistrationCompleteRequest request);
	@DELETE @Path("mfa/registrations/{id}") Response revokeMFA(@PathParam("id") int registrationId);
}
