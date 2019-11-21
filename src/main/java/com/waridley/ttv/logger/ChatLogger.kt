package com.waridley.ttv.logger

import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.events.channel.*
import org.slf4j.LoggerFactory
import java.util.*

open class ChatLogger(val chat: TwitchChat, val channelName: String) {
	var guestChannelName: String? = null
		protected set
	protected var messageLoggers: MutableList<MessageLogger>
	fun addMessageLogger(logger: MessageLogger) {
		messageLoggers.add(logger)
	}
	
	protected fun userJoined(event: ChannelJoinEvent) {
		log.debug("{} just joined {}'s chat!", event.user.name, event.channel.name)
	}
	
	protected fun userLeft(event: ChannelLeaveEvent) {
		log.debug("{} just left {}'s chat... snowpoSOB", event.user.name, event.channel.name)
	}
	
	protected fun onHost(event: HostOnEvent) {
		if (event.channel.name.equals(channelName, ignoreCase = true) && !event.targetChannel.name.equals(guestChannelName, ignoreCase = true)) {
			if (guestChannelName != null) chat.leaveChannel(guestChannelName)
			guestChannelName = event.targetChannel.name
			log.info("Now hosting {}", guestChannelName)
			chat.joinChannel(guestChannelName)
		}
	}
	
	protected fun onUnhost(event: HostOffEvent) {
		log.info("No longer hosting {}", event.channel.name)
		chat.leaveChannel(event.channel.name)
		if (event.channel.name.equals(guestChannelName, ignoreCase = true)) guestChannelName = null
	}
	
	protected fun onRawMsg(event: IRCMessageEvent) {
		log.trace(event.toString())
	}
	
	protected fun logMessage(event: ChannelMessageEvent) {
		for (logger in messageLoggers) {
			logger.logMessage(event)
		}
	}
	
	interface MessageLogger {
		fun logMessage(event: ChannelMessageEvent)
	}
	
	companion object {
		private val log = LoggerFactory.getLogger(ChatLogger::class.java)
	}
	
	init {
		chat.joinChannel(channelName)
		log.info("Joined channel $channelName")
		messageLoggers = ArrayList()
		chat.eventManager.onEvent(ChannelMessageEvent::class.java).subscribe { event: ChannelMessageEvent -> logMessage(event) }
		chat.eventManager.onEvent(HostOnEvent::class.java).subscribe { event: HostOnEvent -> onHost(event) }
		chat.eventManager.onEvent(HostOffEvent::class.java).subscribe { event: HostOffEvent -> onUnhost(event) }
		chat.eventManager.onEvent(ChannelJoinEvent::class.java).subscribe { event: ChannelJoinEvent -> userJoined(event) }
		chat.eventManager.onEvent(ChannelLeaveEvent::class.java).subscribe { event: ChannelLeaveEvent -> userLeft(event) }
		chat.eventManager.onEvent(IRCMessageEvent::class.java).subscribe { event: IRCMessageEvent -> onRawMsg(event) }
	}
}