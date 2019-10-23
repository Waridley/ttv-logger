package com.waridley.ttv.logger.backend.local;

import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.waridley.ttv.logger.ChatLogger;

import java.io.*;

public class FileChatLogger extends ChatLogger {
	PrintWriter logWriter;
	
	public FileChatLogger(TwitchChat chat, String channelName, String filePath) throws IOException {
		super(chat, channelName);
		logWriter = new PrintWriter(new FileWriter(new File(filePath)));
		
	}
	
	@Override
	public void logMessage(ChannelMessageEvent event) {
		logWriter.printf("%s [%s::%s] %s",
				event.getFiredAt(),
				event.getChannel().getName(),
				event.getUser().getName(),
				event.getMessage());
	}
}
