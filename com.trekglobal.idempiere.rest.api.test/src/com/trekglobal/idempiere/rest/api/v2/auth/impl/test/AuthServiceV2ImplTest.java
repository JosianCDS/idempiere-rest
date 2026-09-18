package com.trekglobal.idempiere.rest.api.v2.auth.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.v2.auth.AuthRequestV2;
import com.trekglobal.idempiere.rest.api.v2.auth.ContextRequestV2;
import com.trekglobal.idempiere.rest.api.v2.auth.MFAVerificationRequest;
import com.trekglobal.idempiere.rest.api.v2.auth.MFARegistrationRequest;
import com.trekglobal.idempiere.rest.api.v2.auth.impl.AuthServiceV2Impl;

public class AuthServiceV2ImplTest {
	private AuthServiceV2Impl service;

	@BeforeEach
	void setUp() {
		service = new AuthServiceV2Impl();
	}

	@Test
	void missingCredentialsAreRejectedBeforeCreatingChallenge() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), service.authenticate(new AuthRequestV2()).getStatus());
	}

	@Test
	void missingContextIsRejectedBeforeReadingChallenge() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), service.selectContext(new ContextRequestV2()).getStatus());
	}

	@Test
	void missingTotpFieldsAreRejectedBeforeReadingChallenge() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), service.verifyMFA(new MFAVerificationRequest()).getStatus());
	}

	@Test
	void progressiveEndpointsRejectMissingChallenge() {
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), service.getRoles(null).getStatus());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), service.getOrganizations(null, 1).getStatus());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), service.getWarehouses(null, 1, 1).getStatus());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), service.getLanguage(null).getStatus());
	}

	@Test
	void registrationRequiresMethod() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), service.registerMFA(new MFARegistrationRequest()).getStatus());
	}
}
