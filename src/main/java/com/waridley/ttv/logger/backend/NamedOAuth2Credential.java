/**
 * Copyright (c) 2019 Kevin Day
 * Licensed under the EUPL
 */

package com.waridley.ttv.logger.backend;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;

import java.util.List;

public class NamedOAuth2Credential extends OAuth2Credential {
	
	private String name;
	
	public NamedOAuth2Credential(String name, OAuth2Credential credential) {
		super(credential.getIdentityProvider(), credential.getUserId());
		setName(name);
		setCredential(credential);
	}
	
	@Override
	public String getUserId() {
		
		return credential.getUserId();
	}
	
	@Override
	public String getUserName() {
		return credential.getUserName();
	}
	
	@Override
	public String getAccessToken() {
		return credential.getAccessToken();
	}
	
	@Override
	public String getRefreshToken() {
		return credential.getRefreshToken();
	}
	
	@Override
	public List<String> getScopes() {
		return credential.getScopes();
	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	private OAuth2Credential credential;
	public OAuth2Credential getCredential() { return credential; }
	public void setCredential(OAuth2Credential credential) { this.credential = credential; }
	
	/*
	public AdminCredential(String name, OAuth2Credential credential) {
		this.name = name;
		this.credential = credential;
	}*/
}
