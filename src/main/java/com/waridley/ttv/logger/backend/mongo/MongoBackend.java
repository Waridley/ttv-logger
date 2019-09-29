package com.waridley.ttv.logger.backend.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public interface MongoBackend {
	
	MongoDatabase db();
	
	default <TDocument> MongoCollection<TDocument> createCollectionIfNotExists(String collectionName, Class<TDocument> type) {
		boolean collectionExists = false;
		for(String name : db().listCollectionNames()) {
			if(name.equals(collectionName)) {
				collectionExists = true;
				break;
			}
		}
		if(!collectionExists)  {
			db().createCollection(collectionName);
		}
		return db().getCollection(collectionName, type);
	}
	
}
