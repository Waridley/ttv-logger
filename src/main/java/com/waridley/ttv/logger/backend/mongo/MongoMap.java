package com.waridley.ttv.logger.backend.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
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
	
	@SuppressWarnings("unchecked")
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
		
		return storedMap != null ? (V) storedMap.get(key) : null;
	}
	
	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public Set<Entry<String, V>> entrySet() {
		Set<Entry<String, V>> resultSet = cache.entrySet();
		FindIterable<Document> mapDocs = collection.find(Filters.eq("valueClass", valueClass.toString()));
		for(Document d : mapDocs) {
			Set<Entry<String, Object>> storedSet = d.entrySet();
			for(Entry<String, Object> entry : storedSet) {
				resultSet.add(new MongoEntry<>(entry.getKey(), (V) entry.getValue()));
			}
		}
		
		return resultSet;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		Document value = collection.find(
					Filters.and(
							Filters.eq("valueClass", valueClass.toString()),
							Filters.exists(String.valueOf(key))
					)
		).first() ;
		return value != null ? (V) value.get(key) : null;
	}
	
	
	private static class MongoEntry<V> implements Map.Entry<String, V> {
		
		private String key;
		private V value;
		
		MongoEntry(String key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String getKey() {
			return key;
		}
		
		@Override
		public V getValue() {
			return value;
		}
		
		@Override
		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}
	}
	
}
