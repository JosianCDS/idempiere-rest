package com.trekglobal.idempiere.rest.api.v2.auth;

import com.trekglobal.idempiere.rest.api.v1.auth.LoginParameters;

public class ContextRequestV2 {
	private String challengeToken;
	private LoginParameters parameters;
	public String getChallengeToken() { return challengeToken; }
	public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }
	public LoginParameters getParameters() { return parameters; }
	public void setParameters(LoginParameters parameters) { this.parameters = parameters; }
}
