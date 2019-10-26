package com.waridley.ttv.logger;

import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
public class ChatLogger {
	
	protected final String channelName;
	protected String guestChannelName;
	protected final TwitchChat chat;
	protected List<MessageLogger> messageLoggers;
	
	public ChatLogger(TwitchChat chat, String channelName) {
		this.channelName = channelName;
		this.chat = chat;
		chat.joinChannel(channelName);
		log.info("Joined channel " + channelName);
		
		messageLoggers = new ArrayList<>();
		
		chat.getEventManager().onEvent(ChannelMessageEvent.class).subscribe(this::logMessage);
		chat.getEventManager().onEvent(HostOnEvent.class).subscribe(this::onHost);
		chat.getEventManager().onEvent(HostOffEvent.class).subscribe(this::onUnhost);
		chat.getEventManager().onEvent(ChannelJoinEvent.class).subscribe(this::userJoined);
		chat.getEventManager().onEvent(ChannelLeaveEvent.class).subscribe(this::userLeft);
		chat.getEventManager().onEvent(IRCMessageEvent.class).subscribe(this::onRawMsg);
	}
	
	public void addMessageLogger(MessageLogger logger) {
		messageLoggers.add(logger);
	}
	
	protected synchronized void userJoined(ChannelJoinEvent event) {
		log.debug("{} just joined {}'s chat!", event.getUser().getName(), event.getChannel().getName());
	}
	
	protected synchronized void userLeft(ChannelLeaveEvent event) {
		log.debug("{} just left {}'s chat... snowpoSOB", event.getUser().getName(), event.getChannel().getName());
	}
	
	protected synchronized void onHost(HostOnEvent event) {
		if(event.getChannel().getName().equalsIgnoreCase(channelName) && !event.getTargetChannel().getName().equalsIgnoreCase(guestChannelName)) {
			if(guestChannelName != null) chat.leaveChannel(guestChannelName);
			guestChannelName = event.getTargetChannel().getName();
			log.info("Now hosting {}", guestChannelName);
			chat.joinChannel(guestChannelName);
		}
	}
	
	protected synchronized void onUnhost(HostOffEvent event) {
		log.info("No longer hosting {}", event.getChannel().getName());
		chat.leaveChannel(event.getChannel().getName());
		if(event.getChannel().getName().equalsIgnoreCase(guestChannelName)) guestChannelName = null;
	}
	
	protected synchronized void onRawMsg(IRCMessageEvent event) {
		log.trace(event.toString());
	}
	
	protected void logMessage(ChannelMessageEvent event) {
		for(MessageLogger logger : messageLoggers) {
			logger.logMessage(event);
		}
	}
	
	@FunctionalInterface
	public interface  MessageLogger {
		void logMessage(ChannelMessageEvent event);
	}
}
