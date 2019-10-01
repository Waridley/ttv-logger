package com.waridley.ttv.logger.backend.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MongoMap<V> extends AbstractMap<String, V> {
	
	private Map<String, V> cache = new TreeMap<>();
	private Class<V> valueClass;
	private MongoCollection<Document> collection;
	
	public MongoMap(MongoCollection<Document> collection, Class<V> valueClass) {
		this.valueClass = valueClass;
		
		this.collection = collection;
	}
	
	@Override
	public V put(String key, Object value) {
		Document storedMap = collection.findOneAndUpdate(
				Filters.eq("valueClass", valueClass.toString()),
				Updates.combine(
						Updates.setOnInsert("valueClass", valueClass.toString()),
						Updates.set(key, value)
				),
				new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.BEFORE)
		);
		if(storedMap != null) {
			return collection.getCodecRegistry().get(valueClass).decode(
					new BsonDocumentReader(storedMap.get(key, Document.class).toBsonDocument(valueClass, collection.getCodecRegistry())),
					DecoderContext.builder().build()
			);
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public Set<Entry<String, V>> entrySet() {
		//Set<Entry<String, V>> resultSet = cache.entrySet();
		FindIterable<Document> mapDocs = collection.find(Filters.eq("valueClass", valueClass.toString()), Document.class);
		for(Document d : mapDocs) {
			Set<Entry<String, Object>> storedSet = d.entrySet();
			for(Entry<String, Object> entry : storedSet) {
				cache.put(entry.getKey(), (V) entry.getValue());
			}
		}
		
		return cache.entrySet();
	}
	
	@Override
	public V get(Object key) {
		Document doc = collection.find(
					Filters.and(
							Filters.eq("valueClass", valueClass.toString()),
							Filters.exists(String.valueOf(key))
					)
		).first();
		if(doc != null) {
			return collection.getCodecRegistry().get(valueClass).decode(
					new BsonDocumentReader(doc.get(String.valueOf(key), Document.class).toBsonDocument(valueClass, collection.getCodecRegistry())),
					DecoderContext.builder().build()
			);
		} else {
			return null;
		}
	}
	
}
