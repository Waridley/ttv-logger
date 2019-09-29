/**
 * Copyright (c) 2019 Kevin Day
 * Licensed under the EUPL
 */

package com.waridley.ttv.logger.backend.mongo;

import com.github.philippheuer.credentialmanager.api.IStorageBackend;
import com.github.philippheuer.credentialmanager.domain.Credential;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.waridley.ttv.logger.backend.NamedOAuth2Credential;
import com.waridley.ttv.logger.backend.mongo.codecs.CredentialCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.*;

public class MongoCredentialBackend implements IStorageBackend, MongoBackend {
	
	
	private final MongoDatabase db;
	@Override
	public MongoDatabase db() { return db; }
	
	
	private MongoCollection<Credential> credCollection;
	
	private Map<String, Credential> credentialCache = Collections.synchronizedSortedMap(new TreeMap<>());
	
	
	
	public MongoCredentialBackend(MongoDatabase db, String collectionName) {
		this.db = db;
		/*List<Convention> conventions = new ArrayList<>(Conventions.DEFAULT_CONVENTIONS);
		conventions.add(Conventions.SET_PRIVATE_FIELDS_CONVENTION);
		PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder()
				.automatic(false)
				.conventions(conventions)
				.register(NamedOAuth2Credential.class)
				.build();*/
		CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				com.mongodb.MongoClient.getDefaultCodecRegistry(),
				//CodecRegistries.fromCodecs(new OAuth2Codec()),
				CodecRegistries.fromProviders(new CredentialCodecProvider())
		);
		
		this.credCollection = createCollectionIfNotExists(collectionName, Credential.class).withCodecRegistry(codecRegistry);
	}
	
	@Override
	public List<Credential> loadCredentials() {
		ArrayList<Credential> credentials = new ArrayList<>();
		for(Credential cred : credCollection.find()) {
			credentials.add(cred);
		}
		return credentials;
	}
	
	@Override
	public void saveCredentials(List<Credential> credentials) {
		for(Credential credential : credentials) {
			credCollection.replaceOne(
					Filters.or(
							Filters.eq("userId", credential.getUserId()),
							Filters.eq("name", credential instanceof NamedOAuth2Credential ? ((NamedOAuth2Credential) credential) : null)
							),
					credential,
					new ReplaceOptions().upsert(true)
			);
			
			/*credCollection.findOneAndUpdate(
					Filters.eq("userId", credential.getUserId()),
					Updates.combine(
							new Document(
									"$setOnInsert",
									new Document("userId", credential.getUserId())
							),
							new Document(
									"$set",
									new Document("credential", credential)
							)
					)
			);*/
		}
		
		/*for(Credential credential : credentials) {
			if (credential instanceof OAuth2Credential) {
				CredentialWrapper credentialWrapper;
				Optional<OAuth2Credential> enrichedCredential = provider.getAdditionalCredentialInformation((OAuth2Credential) credential);
				if (enrichedCredential.isPresent()) {
					credentialWrapper = new CredentialWrapper(enrichedCredential.get());
					credCollection.findOneAndUpdate(
							Filters.eq("userId", credential.getUserId()),
							Updates.combine(
									new Document(
											"$setOnInsert",
											new Document("userid", credential.getUserId())
									),
									new Document(
											"$set",
											new Document("credential", credentialWrapper.getCredential())
									)
							),
							new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
					);
				} else {
					throw new RuntimeException("Can't get enriched credential");
				}
			} else {
					throw new RuntimeException("Credential is not an OAuth2Credential");
			}
		}*/
	}
	
	@Override
	public Optional<Credential> getCredentialByUserId(String userId) {
		return Optional.ofNullable(credCollection.find(Filters.eq("userId", userId)).first());
	}
	
	/*public static class CredentialWrapper {
		
		private CredentialWrapper(OAuth2Credential credential) {
			this.setCredential(credential);
			this.setUserId(credential.getUserId());
		}
		
		private String userId;
		public String getUserId() { return userId; }
		private void setUserId(String userId) { this.userId = userId; }
		
		private OAuth2Credential credential;
		public OAuth2Credential getCredential() { return credential; }
		private void setCredential(OAuth2Credential credential) { this.credential = credential; }
	}*/
}
