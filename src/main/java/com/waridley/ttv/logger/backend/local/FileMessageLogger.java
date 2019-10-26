package com.waridley.ttv.logger.backend.local;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.waridley.ttv.logger.ChatLogger;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class FileMessageLogger implements ChatLogger.MessageLogger {
	private PrintWriter logWriter;
	
	public FileMessageLogger(String filePath) throws IOException {
		File file = new File(filePath);
		if(file.getParentFile().mkdirs()) log.info("Created parent directories for log file");
		logWriter = new PrintWriter(new FileOutputStream(file, true));
		log.info("Logging chat to file: {}", filePath);
	}
	
	@Override
	public void logMessage(ChannelMessageEvent event) {
		logWriter.printf("[%s] [%s] [%s] %s\n",
				event.getFiredAt().toInstant().toString(),
				event.getChannel().getName(),
				event.getUser().getName(),
				event.getMessage());
		logWriter.flush();
	}
}
