package com.waridley.ttv.logger.backend.mongo;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.waridley.mongo.MongoBackend;
import com.waridley.ttv.logger.ChatLogger;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;


@Slf4j
public class MongoMessageLogger implements ChatLogger.MessageLogger, MongoBackend {
	
	private final MongoCollection<Document> chatCollection;
	
	private final MongoDatabase db;
	@Override
	public MongoDatabase db() { return db; }
	
	public MongoMessageLogger(MongoDatabase db, String collectionName) {
		this.db = db;
		
		this.chatCollection = createCollectionIfNotExists(collectionName, Document.class);
	}
	
	
	@Override
	public void logMessage(ChannelMessageEvent event) {
//		chatCollection.updateOne(Filters.eq("channelId", event.getChannel().getId().toString()),
//				Updates.push(
//						"chatLog",
//						new Document("time", event.getFiredAt().getTime())
//								.append("userId", event.getUser().getId().toString())
//								.append("message", event.getMessage())
//				),
//				new UpdateOptions().upsert(true)
//		);
		
		chatCollection.insertOne(new Document("time", event.getFiredAt().getTime())
								.append("channelId", event.getChannel().getId())
								.append("userId", event.getUser().getId())
								.append("message", event.getMessage()));
		
		log.debug("Logged message {}", event);
	}
	
}
