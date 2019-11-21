package com.waridley.ttv.logger

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.long
import com.github.philippheuer.credentialmanager.CredentialManager
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.philippheuer.credentialmanager.domain.AuthenticationController
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.events4j.EventManager
import com.github.philippheuer.events4j.domain.Event
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.waridley.credentials.DesktopAuthController
import com.waridley.credentials.NamedCredentialStorageBackend
import com.waridley.credentials.mongo.MongoCredentialStorageBackend
import com.waridley.ttv.RefreshingProvider
import com.waridley.ttv.TtvStorageInterface
import com.waridley.ttv.logger.ChatLogger.MessageLogger
import com.waridley.ttv.logger.NamedCredentialLoader.CredentialConsumer
import com.waridley.ttv.logger.backend.local.FileMessageLogger
import com.waridley.ttv.logger.backend.mongo.MongoMessageLogger
import com.waridley.ttv.mongo.MongoTtvBackend
import org.slf4j.LoggerFactory
import reactor.core.publisher.FluxSink
import reactor.core.publisher.TopicProcessor
import reactor.core.scheduler.Schedulers
import reactor.util.concurrent.WaitStrategy
import java.util.*

class Launcher: CliktCommand() {
	
	private val channelName
			by option("-c", "--channel", help = "The name of the channel to join.").prompt("Channel name:")
	private val clientId
			by option("-i", "--client-id", help = "Your application's client ID").prompt("Client ID:")
	private val clientSecret
			by option("-s", "--client-secret", help = "Your application's client secret.").prompt("Client secret:", hideInput = true)
	private val redirectUrl
			by option("-r", "--redirect-url", help = "The redirect URL for OAuth2 code flow. Must match the URL registered with your client ID.").default("http://localhost")
	private val infoPath
			by option("-p", "--info-path", help = "The path to the page explaining the credential code flow.")
	private val dbConnStr
			by option("-m", "--connection-string", help = "Your MongoDB connection string").prompt("MongoDB connection string:")
	private val chatCollectionName
			by option("-d", "--chat-collection", help = "The name of the MongoDB collection in which to log chat.")
	private val chatLogFilePath
			by option("-f", "--chat-file", help = "The path to a file in which to log chat.")
	private val printChat
			by option("-o", "--print-chat", help = "Print chat to stdout").flag("-n")
	private val intervalMinutes
			by option("-t", "--log-interval").long().default(0L)
	
	private lateinit var idProvider: RefreshingProvider
	private lateinit var db: MongoDatabase
	private lateinit var credBackend: NamedCredentialStorageBackend
	private lateinit var authController: AuthenticationController
	private lateinit var credentialManager: CredentialManager
	
	override fun run() {
		idProvider = RefreshingProvider(clientId, clientSecret, redirectUrl)
		db = connectToDatabase(dbConnStr)
		credBackend = MongoCredentialStorageBackend(db, "credentials")
		authController= DesktopAuthController(if(infoPath != null) redirectUrl + infoPath else null)
		credentialManager = CredentialManagerBuilder.builder()
				.withAuthenticationController(authController)
				.withStorageBackend(credBackend)
				.build()
		
		credentialManager.registerIdentityProvider(idProvider)
		
		val namedCredentialLoader = NamedCredentialLoader(idProvider, object : CredentialConsumer {
			override fun consumeCredential(credential: OAuth2Credential?) {
				onReceivedCredential(credential)
			}
		}, infoPath)
		namedCredentialLoader.startCredentialRetrieval("loggerCredential")
		
	}
	
	private fun onReceivedCredential(credential: OAuth2Credential?) {
		var cred = credential
		val enrichedCred: Optional<OAuth2Credential> = idProvider.getAdditionalCredentialInformation(cred)
		if (enrichedCred.isPresent) {
			cred = enrichedCred.get()
			log.info("Retrieved chat credential for: " + cred.userName)
		}
		credBackend.saveCredential("loggerCredential", cred)
		val eventManager = EventManager(
				Schedulers.newParallel("events4j-scheduler"),
				TopicProcessor.builder<Event>()
						.name("events4j-processor")
						.waitStrategy(WaitStrategy.sleeping())
						.bufferSize(16384)
						.build(),
				FluxSink.OverflowStrategy.BUFFER)
		val twitchClient = TwitchClientBuilder.builder()
				.withEventManager(eventManager)
				.withEnableHelix(true)
				.withEnableTMI(true)
				.withEnableChat(true)
				.withChatAccount(cred)
				.withClientId(clientId)
				.withClientSecret(clientSecret)
				.withRedirectUrl(redirectUrl)
				.withCredentialManager(credentialManager)
				.build()
		twitchClient.clientHelper.enableStreamEventListener(channelName)
		val ttvBackend: TtvStorageInterface = MongoTtvBackend(db, twitchClient.helix, cred)
		//		TwitchChat chat = new TwitchChat(twitchClient.getEventManager(), credentialManager, credential, true, Collections.emptyList());
		val chat = twitchClient.chat
		val chatLogger = ChatLogger(chat, channelName)
		if (chatCollectionName != null) {
			log.debug("Creating MongoDB chat logger: {}", chatCollectionName)
			chatLogger.addMessageLogger(MongoMessageLogger(
					db,
					chatCollectionName))
		}
		if (chatLogFilePath != null) {
			log.debug("Creating FileChatLogger: {}", chatLogFilePath)
			chatLogger.addMessageLogger(FileMessageLogger(
					chatLogFilePath!!))
		}
		if (printChat) {
			log.info("Will print chat to stdout")
			chatLogger.addMessageLogger(object : MessageLogger {
				override fun logMessage(event: ChannelMessageEvent) {
					println(String.format("%s %s... %s", "[" + event.channel.name + "]", String.format("%-25s", event.user.name).replace(' ', '.'), event.message))
				}
			})
		}
		if (intervalMinutes > 0L) WatchtimeLogger(ttvBackend, channelName, intervalMinutes).start()
	}
	
	companion object {
		private val log = LoggerFactory.getLogger(Launcher::class.java)
	}
}

fun connectToDatabase(connectionURI: String): MongoDatabase {
	val connStr = ConnectionString(connectionURI)
	val settings = MongoClientSettings.builder()
			.applyConnectionString(connStr)
			.retryWrites(true)
			.build()
	val mongoClient = MongoClients.create(settings)
	return mongoClient.getDatabase("chatgame")
}

fun main(args: Array<String>) = Launcher().main(args)