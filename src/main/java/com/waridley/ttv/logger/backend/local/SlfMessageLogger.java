package com.waridley.ttv.logger.backend.local;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.waridley.ttv.logger.ChatLogger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlfMessageLogger implements ChatLogger.MessageLogger {
	
	@Override
	public void logMessage(ChannelMessageEvent event) {
		log.info("{} {}... {}", "[" + event.getChannel().getName() + "]", String.format("%-25s", event.getUser().getName()).replace(' ', '.'), event.getMessage());
	}
}
