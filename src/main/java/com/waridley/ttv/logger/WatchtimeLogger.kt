/**
 * Copyright (c) 2019 Kevin Day
 * Licensed under the EUPL
 */
package com.waridley.ttv.logger

import com.github.twitch4j.tmi.TwitchMessagingInterface
import com.github.twitch4j.tmi.TwitchMessagingInterfaceBuilder
import com.waridley.ttv.TtvStorageInterface
import com.waridley.ttv.TtvUser
import com.waridley.ttv.logger.WatchtimeLogger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/* TODO:
 *  Send notitications? Or log events for later handling?
 *  Implement blacklist
 */
class WatchtimeLogger @JvmOverloads constructor(
		private val storageInterface: TtvStorageInterface,
		private val channelName: String,
		intervalMinutes: Long = 10) {
	private var online = false
	private var guestLogin: String?
	private val tmi: TwitchMessagingInterface
	private val channelId: String = storageInterface.getHelixUsersFromLogins(listOf(channelName))[0].id.toString()
	
	private val loggerTask: LoggerTask
	private var scheduler: ScheduledExecutorService? = null
	private var interval: Long
	fun getInterval(): Long {
		return interval
	}
	
	private val lastUpdate: Long
	
	private var running: Boolean
	fun start() {
		if (!running) {
			if (scheduler != null) {
				try {
					scheduler!!.shutdown()
					scheduler!!.awaitTermination(30L, TimeUnit.SECONDS)
				} catch (e: InterruptedException) {
					log.error("Failed to shutdown scheduler:", e)
				}
			}
			scheduler = Executors.newSingleThreadScheduledExecutor()
			scheduler?.scheduleAtFixedRate(loggerTask, 0L, interval, TimeUnit.MINUTES)
			running = true
		} else {
			log.error("WatchtimeLogger already running for {}!", channelName)
		}
	}
	
	fun stop() {
		if (running) {
			if (scheduler != null) {
				try {
					scheduler?.shutdown()
					scheduler?.awaitTermination(30L, TimeUnit.SECONDS)
				} catch (e: Exception) {
					log.error("{}", e)
				}
			} else {
				log.error("Running is true, but scheduler is null!")
			}
		} else {
			log.error("WatchtimeLogger is already stopped!")
		}
		running = false
	}
	
	fun setInterval(minutes: Long) {
		interval = minutes
		if (running) {
			stop()
			start()
		}
	}
	
	private fun updateChatters(): List<TtvUser> {
		val chatters = tmi
				.getChatters(channelName)
				.execute()
		val namesInChat = chatters.allViewers
		namesInChat.add(channelName)
		for (i in namesInChat.indices) {
			namesInChat[i] = namesInChat[i].toLowerCase()
		}
		log.info("{} users in {}'s chat at {}\n    {}", chatters.viewerCount, channelName, Date().toString(), java.lang.String.join("\n    ", namesInChat))
		val helixUsers = storageInterface.getHelixUsersFromLogins(namesInChat)
		if (helixUsers.size != namesInChat.size) {
			log.trace("Names in guest chat: {} | Helix users found: {}", namesInChat.size, helixUsers.size)
			log.trace("Helix users:")
			for (u in helixUsers) {
				log.trace("    {}", u.login)
			}
		}
		return storageInterface.findTtvUsers(helixUsers)
	}
	
	private fun updateGuests(): List<TtvUser> {
		var guests = emptyList<TtvUser>()
		for (host in tmi.getHosts(listOf(channelId)).execute().hosts) {
			if (host.targetLogin != null) {
				enterHostMode(host.targetLogin)
				val guestChatters = tmi
						.getChatters(guestLogin)
						.execute()
				val namesInGuestChat = guestChatters.allViewers
				namesInGuestChat.add(guestLogin)
				for (i in namesInGuestChat.indices) {
					namesInGuestChat[i] = namesInGuestChat[i]!!.toLowerCase()
				}
				log.info("{} users watching {} at {}\n    {}", guestChatters.viewerCount, guestLogin, Date().toString(), java.lang.String.join("\n    ", namesInGuestChat))
				val guestHelixUsers = storageInterface.getHelixUsersFromLogins(namesInGuestChat)
				if (guestHelixUsers.size != namesInGuestChat.size) {
					log.trace("Names in guest chat: {} | Helix users found: {}", namesInGuestChat.size, guestHelixUsers.size)
					log.trace("Helix users:")
					for (u in guestHelixUsers) {
						log.trace("    {}", u.displayName)
					}
				}
				guests = storageInterface.findTtvUsers(guestHelixUsers)
			} else {
				exitHostMode()
				guests = emptyList()
			}
		}
		return guests
	}
	
	private fun updateHosts(): List<TtvUser> {
		val hostingChannels = Vector<TtvUser>()
		if (online) {
			for (host in tmi.getHostsOf(channelId).execute().hosts) {
				val hostingUser = storageInterface.findOrCreateTtvUserFromId(host.hostId)
				hostingChannels.add(hostingUser)
			}
		}
		return hostingChannels
	}
	
	private fun logMinutes(user: TtvUser, minutes: Long): TtvUser {
		return storageInterface.logMinutes(user, minutes, online)
	}
	
	private fun logGuestMinutes(user: TtvUser, minutes: Long): TtvUser {
		return storageInterface.logGuestMinutes(user, minutes, guestLogin)
	}
	
	private fun logHostingMinutes(user: TtvUser, minutes: Long): TtvUser {
		//TODO implement hosting minutes logging
		return user
	}
	
	private fun logAllMinutes(minutes: Long) {
		for (u in updateChatters()) {
			logMinutes(u, minutes)
		}
		for (u in updateGuests()) {
			logGuestMinutes(u, minutes)
		}
		for (u in updateHosts()) {
			logHostingMinutes(u, minutes)
		}
	}
	
	private fun goOnline(title: String, gameId: String) {
		online = true
		if (guestLogin != null) exitHostMode()
		log.info("{} is streaming! Title: {}", channelName, title)
	}
	
	private fun goOffline() {
		online = false
		log.info("{} is offline.", channelName)
	}
	
	private fun enterHostMode(targetChannelName: String) {
		if (targetChannelName.equals(guestLogin, ignoreCase = true)) {
			log.info("Currently hosting {}", guestLogin)
		} else {
			exitHostMode()
			log.info("Now hosting {}", targetChannelName)
			guestLogin = targetChannelName
		}
	}
	
	private fun exitHostMode() {
		if (guestLogin != null) {
			log.info("No longer hosting {}", guestLogin)
			guestLogin = null
		} else {
			log.info("Not currently hosting anyone.")
		}
	}
	
	private fun checkOnline() {
		val streams = storageInterface.helix().getStreams(
				storageInterface.helixAccessToken(),
				"", null, 1, null, null, null, null, listOf(channelName)).execute().streams
		if (streams.size == 1) { //channel is streaming?
			val stream = streams[0]
			if (stream.type.equals("live", ignoreCase = true)) {
				goOnline(stream.title, stream.gameId)
			} else {
				log.error("Stream found but type is: {}", stream.type)
				goOffline()
			}
		} else {
			goOffline()
		}
	}
	
	private inner class LoggerTask : Runnable {
		val parent = this@WatchtimeLogger
		override fun run() {
			try {
				parent.checkOnline()
				parent.logAllMinutes(parent.getInterval())
			} catch (e: Exception) {
				log.error("Error: ", e)
			}
		}
	}
	
	companion object {
		private val log = LoggerFactory.getLogger(WatchtimeLogger::class.java)
	}
	
	/**
	 * Constructs a WatchtimeLogger with the default interval of 10 minutes
	 */
	init {
		interval = intervalMinutes
		loggerTask = LoggerTask()
		lastUpdate = 0L
		running = false
		guestLogin = null
		tmi = TwitchMessagingInterfaceBuilder.builder().build()
	}
}