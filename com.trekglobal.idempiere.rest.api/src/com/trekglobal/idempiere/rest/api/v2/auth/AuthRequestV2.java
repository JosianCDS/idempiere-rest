package com.trekglobal.idempiere.rest.api.v2.auth;

import com.trekglobal.idempiere.rest.api.v1.auth.LoginParameters;

public class AuthRequestV2 {
	private String userName;
	private String password;
	private LoginParameters parameters;
	public String getUserName() { return userName; }
	public void setUserName(String userName) { this.userName = userName; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public LoginParameters getParameters() { return parameters; }
	public void setParameters(LoginParameters parameters) { this.parameters = parameters; }
}
