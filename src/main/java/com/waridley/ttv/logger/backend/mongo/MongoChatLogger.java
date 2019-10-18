package com.waridley.ttv.logger.backend.mongo;

import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.waridley.mongo.MongoBackend;
import com.waridley.ttv.logger.ChatLogger;
import org.bson.Document;

public class MongoChatLogger extends ChatLogger implements MongoBackend {
	
	private final MongoCollection<Document> chatCollection;
	
	private final MongoDatabase db;
	@Override
	public MongoDatabase db() { return db; }
	
	public MongoChatLogger(TwitchChat chat, String channelName, MongoDatabase db, String collectionName) {
		super(chat, channelName);
		this.db = db;
		
		this.chatCollection = createCollectionIfNotExists(collectionName, Document.class);
	}
	
	@Override
	protected void onMessage(ChannelMessageEvent event) {
		super.onMessage(event);
		logMessage(event);
	}
	
	@Override
	public void logMessage(ChannelMessageEvent event) {
		chatCollection.updateOne(Filters.eq("channelId", event.getChannel().getId().toString()),
				Updates.push(
						"chatLog",
						new Document("time", event.getFiredAt().getTime())
								.append("userId", event.getUser().getId().toString())
								.append("message", event.getMessage())
				),
				new UpdateOptions().upsert(true)
		);
	}
	
}
