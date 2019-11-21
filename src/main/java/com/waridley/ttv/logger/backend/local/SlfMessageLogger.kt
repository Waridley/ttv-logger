package com.waridley.ttv.logger.backend.local

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.waridley.ttv.logger.ChatLogger.MessageLogger
import org.slf4j.LoggerFactory

class SlfMessageLogger : MessageLogger {
	override fun logMessage(event: ChannelMessageEvent) {
		log.info("[{}] {}... {}", event.channel.name, String.format("%-25s", event.user.name).replace(' ', '.'), event.message)
	}
	
	companion object {
		private val log = LoggerFactory.getLogger(SlfMessageLogger::class.java)
	}
}