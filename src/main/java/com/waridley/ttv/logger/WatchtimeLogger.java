/**
 * Copyright (c) 2019 Kevin Day
 * Licensed under the EUPL
 */

package com.waridley.ttv.logger;

import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.tmi.TwitchMessagingInterface;
import com.github.twitch4j.tmi.TwitchMessagingInterfaceBuilder;
import com.github.twitch4j.tmi.domain.Host;
import com.waridley.ttv.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* TODO:
 *  Send notitications? Or log events for later handling?
 *  Implement blacklist
 */

public class WatchtimeLogger {
	
	private boolean online;
	private String guestLogin;
	
	
	private TwitchMessagingInterface tmi;
	private TtvStorageInterface storageInterface;
	private String channelName;
	private String channelId;
	
	private List<TtvUser> usersInChat = Collections.emptyList();
	public List<TtvUser> getUsersInChat() { return usersInChat; }
	private List<TtvUser> guestViewers = Collections.emptyList();
	public List<TtvUser> getGuestViewers() { return guestViewers; }
	
	private LoggerTask loggerTask;
	private ScheduledExecutorService scheduler;
	private long interval;
	public long getInterval() { return interval; }
	private long lastUpdate;
	public long getLastUpdate() { return lastUpdate; }
	private boolean running;
	
	/**
	 * Constructs a WatchtimeLogger with the default interval of 10 minutes
	 */
	public WatchtimeLogger (
			TtvStorageInterface storageInterface,
			String channelName) {
		this(storageInterface, channelName, 10);
	}
	
	public WatchtimeLogger (
			TtvStorageInterface storageInterface,
			String channelName,
			long intervalMinutes) {
		
		this.storageInterface = storageInterface;
		this.channelName = channelName;
		this.channelId = storageInterface.getHelixUsersFromLogins(Collections.singletonList(channelName)).get(0).getId().toString();
		this.interval = intervalMinutes;
		this.loggerTask = new LoggerTask();
		this.lastUpdate = 0L;
		this.running = false;
		this.guestLogin = null;
		
		this.tmi = TwitchMessagingInterfaceBuilder.builder().build();
		
		
	}
	
	public void start() {
		if(!running) {
			if(scheduler != null) {
				try {
					scheduler.shutdown();
					scheduler.awaitTermination(30L, TimeUnit.SECONDS);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.scheduleAtFixedRate(loggerTask, 0L, this.interval, TimeUnit.MINUTES);
			running = true;
		} else {
			System.err.println("WatchtimeLogger already running for " + channelName + "!");
		}
	}
	
	public void stop() {
		if(running) {
			if(scheduler != null) {
				try {
					scheduler.shutdown();
					scheduler.awaitTermination(30L, TimeUnit.SECONDS);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Running is true, but scheduler is null!");
			}
		} else {
			System.err.println("WatchtimeLogger is already stopped!");
		}
		running = false;
	}
	
	public synchronized void setInterval(long minutes) {
		this.interval = minutes;
		if(running) {
			stop();
			start();
		}
	}
	
	private synchronized void updateChatters() {
		//Update no more often than 30 seconds
		if(new Date().getTime() - getLastUpdate() >= 30 * 1000) {
			List<String> namesInChat = tmi
							.getChatters(this.channelName)
							.execute()
							.getAllViewers();
			namesInChat.add(channelName);
			for(int i = 0; i < namesInChat.size(); i++) {
				namesInChat.set(i, namesInChat.get(i).toLowerCase());
			}
			List<TtvUser> tmpUsers = new Vector<>();
			
			System.out.println("Users in chat at " + new Date().toString());
			//tmpUsers = storageInterface.findOrCreateTtvUsers(storageInterface.getHelixUsers(null, namesInChat));
			
			List<User> helixUsers = storageInterface.getHelixUsersFromLogins(namesInChat);
			for(int i = 0; i < namesInChat.size(); i++) {
				tmpUsers.add(storageInterface.findOrCreateTtvUser(helixUsers.get(i)));
				//System.out.println("    " + (tmpUsers.get(i).getHelixUser() != null ? (tmpUsers.get(i).getHelixUser().getDisplayName()) : namesInChat.get(i)));
			}
			for(TtvUser u : tmpUsers) {
				System.out.println("    " + u.getHelixUser().getDisplayName());
			}

			this.usersInChat = tmpUsers;
			
			
			try {
				for(Host host : tmi.getHosts(Collections.singletonList(channelId)).execute().getHosts()) {
					if(host.getTargetLogin() != null) {
						enterHostMode(host.getTargetLogin());
						List<String> namesInGuestChat = tmi
								.getChatters(guestLogin)
								.execute()
								.getAllViewers();
						namesInGuestChat.add(guestLogin);
						for(int i = 0; i < namesInGuestChat.size(); i++) {
							namesInGuestChat.set(i, namesInGuestChat.get(i).toLowerCase());
						}
						
						List<TtvUser> tmpGuests = new Vector<>();
						
						System.out.println("Users watching " + guestLogin + " at " + new Date().toString());
						//tmpGuests = storageInterface.findOrCreateTtvUsers(storageInterface.getHelixUsers(null, namesInGuestChat));
						List<User> guestHelixUsers = storageInterface.getHelixUsersFromLogins(namesInGuestChat);
						for(int i = 0; i < namesInGuestChat.size(); i++) {
							tmpGuests.add(storageInterface.findOrCreateTtvUser(guestHelixUsers.get(i)));
							//System.out.println("    " + (tmpGuests.get(i).getHelixUser() != null ? (tmpGuests.get(i).getHelixUser().getDisplayName()) : namesInGuestChat.get(i)));
						}
						for(TtvUser u : tmpGuests) {
							System.out.println("    " + u.getHelixUser().getDisplayName());
						}
	
						this.guestViewers = tmpGuests;
					} else {
						exitHostMode();
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			lastUpdate = new Date().getTime();
			
		} else {
			throw new RuntimeException("Attempting to update chatters less than 30s after " + lastUpdate);
		}
	}
	
	private synchronized TtvUser logMinutes(TtvUser user, long minutes) {
		return storageInterface.logMinutes(user, minutes, online);
	}
	
	private synchronized TtvUser logGuestMinutes(TtvUser user, long minutes) {
		return storageInterface.logGuestMinutes(user, minutes, guestLogin);
	}
	
	private synchronized void logAllMinutes(long minutes) {
		List<TtvUser> viewers = getUsersInChat();
		for(int i = 0; i < viewers.size(); i++) {
			//System.out.println(viewers.get(i).getHelixUser().getDisplayName() + " had " + String.format("%.2f", TtvUser.toHours(viewers.get(i).channelMinutes())) + "h");
			viewers.set(i, logMinutes(viewers.get(i), minutes));
			//System.out.println(viewers.get(i).getHelixUser().getDisplayName() + " now has " + String.format("%.2f", TtvUser.toHours(viewers.get(i).channelMinutes())) + "h");
		}
		List<TtvUser> guestViewers = getGuestViewers();
		for(int i = 0; i < guestViewers.size(); i++) {
			//System.out.println(guestViewers.get(i).getHelixUser().getDisplayName() + " had " + String.format("%.2f", TtvUser.toHours(guestViewers.get(i).getGuestMinutes())) + " guest hours");
			guestViewers.set(i, logGuestMinutes(guestViewers.get(i), minutes));
			//System.out.println(guestViewers.get(i).getHelixUser().getDisplayName() + " now has " + String.format("%.2f", TtvUser.toHours(guestViewers.get(i).getGuestMinutes())) + " guest hours");
		}
	}
	
	private synchronized void goOnline(String title, String gameId) {
		online = true;
		if(guestLogin != null) exitHostMode();
		System.out.println(channelName + " is streaming! Title: " + title);
	}
	
	private synchronized void goOffline() {
		online = false;
		System.out.println(channelName + " is offline.");
	}
	
	private synchronized void enterHostMode(String targetChannelName) {
		if(targetChannelName.equalsIgnoreCase(guestLogin)) {
			System.out.println("Currently hosting " + guestLogin);
		} else {
			exitHostMode();
			System.out.println("Now hosting " + targetChannelName);
			guestLogin = targetChannelName;
		}
	}
	
	private synchronized void exitHostMode() {
		if(guestLogin != null) {
			System.out.println("No longer hosting " + guestLogin);
		}
		guestLogin = null;
	}
	
	private synchronized void checkOnline() {
		List<Stream> streams = storageInterface.helix().getStreams(
				storageInterface.helixAccessToken(),
				"", null, 1, null, null, null, null,
				Collections.singletonList(channelName)
		).execute().getStreams();
		if(streams.size() == 1) { //channel is streaming?
			Stream stream = streams.get(0);
			if(stream.getType().equalsIgnoreCase("live")) {
				goOnline(stream.getTitle(), stream.getGameId());
			} else {
				System.out.println("Stream found but type is: " + stream.getType());
				goOffline();
			}
		} else {
			goOffline();
		}
	}
	
	private class LoggerTask implements Runnable {
		WatchtimeLogger parent = WatchtimeLogger.this;
		
		@Override
		public void run() {
			try {
				//System.out.println("Checking if channel is online");
				parent.checkOnline();
				//System.out.println("Updating chatters");
				parent.updateChatters();
				//System.out.println("Logging all minutes");
				parent.logAllMinutes(parent.getInterval());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
