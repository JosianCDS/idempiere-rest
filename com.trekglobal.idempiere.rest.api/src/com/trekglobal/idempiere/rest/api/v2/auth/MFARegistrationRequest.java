package com.trekglobal.idempiere.rest.api.v2.auth;

public class MFARegistrationRequest {
	private int methodId;
	private String name;
	public int getMethodId() { return methodId; }
	public void setMethodId(int methodId) { this.methodId = methodId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
}
