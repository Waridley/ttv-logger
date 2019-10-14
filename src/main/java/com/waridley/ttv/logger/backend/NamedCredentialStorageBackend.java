package com.waridley.ttv.logger.backend;

import com.github.philippheuer.credentialmanager.api.IStorageBackend;
import com.github.philippheuer.credentialmanager.domain.Credential;

import java.util.*;

public class NamedCredentialStorageBackend implements IStorageBackend {
	
	protected final Map<String, Credential> credentialMap;
	
	
	public NamedCredentialStorageBackend() {
		this.credentialMap = Collections.synchronizedSortedMap(new TreeMap<>());
	}
	
	public NamedCredentialStorageBackend(Map<String, Credential> backingMap) {
		this.credentialMap = backingMap;
	}
	
	public Credential getCredentialByName(String name) {
		return credentialMap.get(name);
	}
	
	public void storeNamedCredential(String name, Credential credential) {
		credentialMap.put(name, credential);
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
			credentialMap.put(c.getIdentityProvider() + ":" + c.getUserId() + ":" + c.getClass(), c);
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
