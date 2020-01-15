package com.waridley.ttv.logger.backend.local

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.waridley.ttv.logger.ChatLogger.MessageLogger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

class FileMessageLogger(filePath: String) : MessageLogger {
	private val logWriter: PrintWriter
	override fun logMessage(event: ChannelMessageEvent) {
		logWriter.printf("[%s] [%s] [%s] %s\n",
				event.firedAt.toInstant().toString(),
				event.channel.name,
				event.user.name,
				event.message)
		logWriter.flush()
	}
	
	companion object {
		private val log = LoggerFactory.getLogger(FileMessageLogger::class.java)
	}
	
	init {
		val file = File(filePath)
		if (file.parentFile?.mkdirs() == true) log.info("Created parent directories for log file")
		logWriter = PrintWriter(FileOutputStream(file, true))
		log.info("Logging chat to file: {}", filePath)
	}
}