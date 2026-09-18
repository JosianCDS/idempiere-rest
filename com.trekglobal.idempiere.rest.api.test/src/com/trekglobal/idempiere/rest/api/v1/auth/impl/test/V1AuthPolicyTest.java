package com.trekglobal.idempiere.rest.api.v1.auth.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import javax.ws.rs.core.Response;

import org.compiere.model.MSysConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.trekglobal.idempiere.rest.api.v1.auth.LoginCredential;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginParameters;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;

public class V1AuthPolicyTest {

	@Test
	void v1TokenEndpointsReturnGoneWhenAuthenticationIsDisabled() {
		try (MockedStatic<MSysConfig> config = mockStatic(MSysConfig.class)) {
			config.when(() -> MSysConfig.getBooleanValue(AuthServiceImpl.REST_V1_AUTH_ENABLED, true)).thenReturn(false);
			AuthServiceImpl service = new AuthServiceImpl();

			Response authenticate = service.authenticate(new LoginCredential());
			Response changeContext = service.changeLoginParameters(new LoginParameters());

			assertDisabled(authenticate);
			assertDisabled(changeContext);
		}
	}

	private void assertDisabled(Response response) {
		assertEquals(Response.Status.GONE.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity().toString().contains("v1_auth_disabled"));
		assertTrue(response.getEntity().toString().contains("/api/v2/auth/tokens"));
	}
}
