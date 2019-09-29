package com.waridley.ttv.logger.backend.mongo;

import com.github.philippheuer.credentialmanager.domain.Credential;
import com.mongodb.client.MongoDatabase;
import com.waridley.ttv.logger.backend.NamedCredentialMap;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class MongoNamedCredentialMap extends NamedCredentialMap implements MongoBackend {
	
	
	private final MongoDatabase db;
	
	@Override
	public MongoDatabase db() { return db; }
	
	public MongoNamedCredentialMap(MongoDatabase db) {
		this.db = db;
	}
	
	
	@Override
	public Credential getCredentialByName(String name) {
		//TODO check cache then load from db if not there
		return super.getCredentialByName(name);
	}
	
	@Override
	public void storeNamedCredential(String name, Credential credential) {
		super.storeNamedCredential(name, credential);
		
	}
	
	@Override
	public List<Credential> loadCredentials() {
		//TODO put load credentials from db into credentialMap
		
		return new Vector<>(credentialMap.values());
	}
	
	@Override
	public void saveCredentials(List<Credential> credentials) {
		for(Credential c : credentials) {
			credentialMap.put(String.valueOf(c.getUserId()), c);
		}
		
		//TODO save to db
	}
	
	@Override
	public Optional<Credential> getCredentialByUserId(String userId) {
		for(Credential c : credentialMap.values()) {
			if(c.getUserId().equals(userId)) return Optional.of(c);
		}
		return Optional.empty();
	}
	
}
