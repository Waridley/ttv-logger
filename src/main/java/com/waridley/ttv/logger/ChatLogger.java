package com.waridley.ttv.logger;

import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public abstract class ChatLogger {
	
	protected final String channelName;
	protected String guestChannelName;
	protected final TwitchChat chat;
	
	public ChatLogger(TwitchChat chat, String channelName) {
		this.channelName = channelName;
		this.chat = chat;
		chat.joinChannel(channelName);
		log.info("Joined channel " + channelName);
		chat.getEventManager().onEvent(ChannelMessageEvent.class).subscribe(this::onMessage);
		chat.getEventManager().onEvent(HostOnEvent.class).subscribe(this::onHost);
		chat.getEventManager().onEvent(HostOffEvent.class).subscribe(this::onUnhost);
		chat.getEventManager().onEvent(ChannelJoinEvent.class).subscribe(this::userJoined);
		chat.getEventManager().onEvent(ChannelLeaveEvent.class).subscribe(this::userLeft);
		chat.getEventManager().onEvent(IRCMessageEvent.class).subscribe(this::onRawMsg);
	}
	
	
	protected void onMessage(ChannelMessageEvent event) {
		log.info("[{}::{}] {}", event.getChannel().getName(), event.getUser().getName(), event.getMessage());
	}
	
	protected synchronized void userJoined(ChannelJoinEvent event) {
		log.info("{} just joined {}'s chat!", event.getUser().getName(), event.getChannel().getName());
	}
	
	protected synchronized void userLeft(ChannelLeaveEvent event) {
		log.info("{} just left {}'s chat... snowpoSOB", event.getUser().getName(), event.getChannel().getName());
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
		log.info("No longer hosting {}", guestChannelName);
		chat.leaveChannel(guestChannelName);
		guestChannelName = null;
	}
	
	protected synchronized void onRawMsg(IRCMessageEvent event) {
		log.trace(event.toString());
	}
	
	public abstract void logMessage(ChannelMessageEvent event);
}
