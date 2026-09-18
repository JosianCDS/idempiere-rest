package com.trekglobal.idempiere.rest.api.v2.auth;

public class MFAVerificationRequest {
	private String challengeToken;
	private int registrationId;
	private String code;
	public String getChallengeToken() { return challengeToken; }
	public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }
	public int getRegistrationId() { return registrationId; }
	public void setRegistrationId(int registrationId) { this.registrationId = registrationId; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
}
