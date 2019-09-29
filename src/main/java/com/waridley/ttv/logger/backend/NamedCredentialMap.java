package com.waridley.ttv.logger.backend;

import com.github.philippheuer.credentialmanager.api.IStorageBackend;
import com.github.philippheuer.credentialmanager.domain.Credential;

import java.util.*;

public class NamedCredentialMap implements IStorageBackend {
	
	protected final Map<String, Credential> credentialMap = Collections.synchronizedSortedMap(new TreeMap<>());
	
	public Credential getCredentialByName(String name) {
		return credentialMap.get(name);
	}
	
	public void storeNamedCredential(String name, Credential credential) {
		if(credential instanceof NamedOAuth2Credential && ((NamedOAuth2Credential) credential).getName().equals(name)) credentialMap.put(name, ((NamedOAuth2Credential) credential).getCredential());
		else credentialMap.put(name, credential);
	}
	
	public String getNameOf(Credential credential) {
		for(Map.Entry<String, Credential> entry : credentialMap.entrySet()) {
			if(entry.getValue().equals(credential)) return entry.getKey();
		}
		return null;
	}
	
	@Override
	public List<Credential> loadCredentials() {
		return new Vector<>(credentialMap.values());
	}
	
	@Override
	public void saveCredentials(List<Credential> credentials) {
		for(Credential c : credentials) {
			if(c instanceof NamedOAuth2Credential) credentialMap.put(((NamedOAuth2Credential) c).getName(), ((NamedOAuth2Credential) c).getCredential());
			else credentialMap.put(c.getIdentityProvider() + ":" + c.getUserId() + ":" + c.getClass(), c);
		}
	}
	
	@Override
	public Optional<Credential> getCredentialByUserId(String userId) {
		for(Credential c : credentialMap.values()) {
			if(c.getUserId().equals(userId)) return Optional.of(c);
		}
		return Optional.empty();
	}
}
