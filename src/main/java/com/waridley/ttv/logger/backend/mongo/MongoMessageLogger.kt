package com.waridley.ttv.logger.backend.mongo

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.waridley.mongo.MongoBackend
import com.waridley.ttv.logger.ChatLogger.MessageLogger
import org.bson.Document
import org.slf4j.LoggerFactory

class MongoMessageLogger(private val db: MongoDatabase, collectionName: String?) : MessageLogger, MongoBackend {
	private val chatCollection: MongoCollection<Document> = createCollectionIfNotExists(collectionName, Document::class.java)
	override fun db(): MongoDatabase {
		return db
	}
	
	override fun logMessage(event: ChannelMessageEvent) {
//		chatCollection.updateOne(Filters.eq("channelId", event.getChannel().getId().toString()),
//				Updates.push(
//						"chatLog",
//						new Document("time", event.getFiredAt().getTime())
//								.append("userId", event.getUser().getId().toString())
//								.append("message", event.getMessage())
//				),
//				new UpdateOptions().upsert(true)
//		);
		chatCollection.insertOne(Document("time", event.firedAt.time)
				.append("channelId", event.channel.id)
				.append("userId", event.user.id)
				.append("message", event.message))
		log.debug("Logged message {}", event)
	}
	
	companion object {
		private val log = LoggerFactory.getLogger(MongoMessageLogger::class.java)
	}
	
}