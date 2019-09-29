package com.waridley.ttv.logger;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.api.IStorageBackend;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.waridley.ttv.TMIHostGetter;
import com.waridley.ttv.logger.backend.DesktopAuthController;
import com.waridley.ttv.logger.backend.NamedCredentialMap;
import com.waridley.ttv.logger.backend.RefreshingProvider;
import com.waridley.ttv.TtvStorageInterface;
import com.waridley.ttv.logger.backend.mongo.MongoChatLogger;
import com.waridley.ttv.logger.backend.mongo.MongoTtvBackend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;

public class Launcher {
	
	private static String redirectUrl = "http://localhost:6464";
	private static String channelName;
	private static String channelId = "43394066";
	private static String clientId;
	private static String clientSecret;
	private static RefreshingProvider idProvider;
	private static MongoDatabase db;
	private static NamedCredentialMap credMap;
	private static IStorageBackend credBackend;
	private static CredentialManager credentialManager;
	private static TwitchClient twitchClient;
	private static TMIHostGetter tmiHostGetter;
	private static TtvStorageInterface ttvBackend;
	private static long intervalMinutes = 6L;
	
	public static void main(String[] args) {
		try {
			init(args);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private synchronized static void startWatchtimeLogger() {
		WatchtimeLogger logger = new WatchtimeLogger(ttvBackend, channelName, intervalMinutes);
		logger.start();
		
	}
	
	private synchronized static void init(String[] args) throws URISyntaxException, IOException {
		LoggerOptions options = createOptionsFromArgs(args);
		channelName = args[0];
		clientId = args[2];
		clientSecret = args[3];
		idProvider = new RefreshingProvider(clientId, clientSecret, redirectUrl);
		db = connectToDatabase(args[4]);
		
		credMap = new NamedCredentialMap();
		credBackend = credMap;
		
		DesktopAuthController authController = new DesktopAuthController(redirectUrl + "/info.html");
		
		credentialManager = CredentialManagerBuilder.builder()
				.withAuthenticationController(authController)
				.withStorageBackend(credBackend)
				.build();
		credentialManager.registerIdentityProvider(idProvider);
		
		NamedCredentialLoader namedCredentialLoader = new NamedCredentialLoader(idProvider, Launcher::onReceivedCredential);
		namedCredentialLoader.startCredentialRetrieval("credential");
		
	}
	
	private synchronized static void onReceivedCredential(OAuth2Credential credential) {
		Optional<OAuth2Credential> enrichedCred = idProvider.getAdditionalCredentialInformation(credential);
		if(enrichedCred.isPresent()) {
			credential = enrichedCred.get();
			System.out.println("Retrieved chat credential for: " + credential.getUserName());
		}
		twitchClient = TwitchClientBuilder.builder()
				.withEnableHelix(true)
				.withEnableTMI(true)
				.withEnableChat(true)
				.withClientId(clientId)
				.withClientSecret(clientSecret)
				.withRedirectUrl(redirectUrl)
				.withCredentialManager(credentialManager)
				.withChatAccount(credential)
				.build();
		twitchClient.getClientHelper().enableStreamEventListener(channelName);
		
		ttvBackend = new MongoTtvBackend(db, twitchClient.getHelix(), credential);
		
		
		System.out.println("Channel ID: " + ttvBackend.getHelixUsersFromLogins(Collections.singletonList(channelName)).get(0).getId().toString());
		System.out.println("Creating chatLogger for " + channelName);
		ChatLogger chatLogger = new MongoChatLogger(
				twitchClient.getChat(),
				channelName,
				channelId,
				db,
				"chat");
		startWatchtimeLogger();
	}
	
	public static MongoDatabase connectToDatabase(String connectionURI) {
		ConnectionString connStr = new ConnectionString(connectionURI);
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(connStr)
				.retryWrites(true)
				.build();
		
		MongoClient mongoClient = MongoClients.create(settings);
		return mongoClient.getDatabase("chatgame");
	}
	
	private synchronized static LoggerOptions createOptionsFromArgs(String[] args) {
		return new LoggerOptionsBuilder()
				.withStorageInterface(ttvBackend)
				.build();
	}
	
	
}