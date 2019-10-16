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
		System.out.println("Joined channel " + channelName);
		chat.getEventManager().onEvent(ChannelMessageEvent.class).subscribe(this::onMessage);
		chat.getEventManager().onEvent(HostOnEvent.class).subscribe(this::onHost);
		chat.getEventManager().onEvent(HostOffEvent.class).subscribe(this::onUnhost);
		chat.getEventManager().onEvent(ChannelJoinEvent.class).subscribe(this::userJoined);
		chat.getEventManager().onEvent(ChannelLeaveEvent.class).subscribe(this::userLeft);
	}
	
	
	protected void onMessage(ChannelMessageEvent event) {
		System.out.println("[" + event.getChannel().getName() + "]:" + event.getUser().getName() + ": " + event.getMessage());
	}
	
	protected synchronized void userJoined(ChannelJoinEvent event) {
		System.out.println(event.getUser().getName() + " just joined " + event.getChannel().getName() + "'s chat!");
	}
	
	protected synchronized void userLeft(ChannelLeaveEvent event) {
		System.out.println(event.getUser().getName() + " just left " + event.getChannel().getName() + "'s chat... snowpoSOB");
	}
	
	protected synchronized void onHost(HostOnEvent event) {
		if(event.getChannel().getName().equalsIgnoreCase(channelName) && !event.getTargetChannel().getName().equalsIgnoreCase(guestChannelName)) {
			if(guestChannelName != null) chat.leaveChannel(guestChannelName);
			guestChannelName = event.getTargetChannel().getName();
			System.out.println("Now hosting " + guestChannelName);
			chat.joinChannel(guestChannelName);
		}
	}
	
	protected synchronized void onUnhost(HostOffEvent event) {
		System.out.println("No longer hosting " + guestChannelName);
		chat.leaveChannel(guestChannelName);
		guestChannelName = null;
	}
	
	public abstract void logMessage(ChannelMessageEvent event);
}
