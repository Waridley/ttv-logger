package com.waridley.ttv.logger.backend.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public interface MongoBackend {
	
	MongoDatabase db();
	
	default <TDocument> MongoCollection<TDocument> createCollectionIfNotExists(String collectionName, Class<TDocument> documentClass) {
		return createCollectionIfNotExists(db(), collectionName, documentClass);
	}
	
	static <TDocument> MongoCollection<TDocument> createCollectionIfNotExists(MongoDatabase db, String collectionName, Class<TDocument> documentClass) {
		boolean collectionExists = false;
		for(String name : db.listCollectionNames()) {
			if(name.equals(collectionName)) {
				collectionExists = true;
				break;
			}
		}
		if(!collectionExists)  {
			db.createCollection(collectionName);
		}
		return db.getCollection(collectionName, documentClass);
	}
	
}
