package com.trekglobal.idempiere.rest.api.v2.auth;

public class MFARegistrationCompleteRequest {
	private String code;
	private String name;
	private boolean preferred;
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public boolean isPreferred() { return preferred; }
	public void setPreferred(boolean preferred) { this.preferred = preferred; }
}
