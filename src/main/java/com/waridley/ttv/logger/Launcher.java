package com.waridley.ttv.logger;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.api.IStorageBackend;
import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.waridley.ttv.logger.backend.DesktopAuthController;
import com.waridley.ttv.logger.backend.NamedCredentialStorageBackend;
import com.waridley.ttv.logger.backend.RefreshingProvider;
import com.waridley.ttv.TtvStorageInterface;
import com.waridley.ttv.logger.backend.mongo.MongoBackend;
import com.waridley.ttv.logger.backend.mongo.MongoChatLogger;
import com.waridley.ttv.logger.backend.mongo.MongoMap;
import com.waridley.ttv.logger.backend.mongo.MongoTtvBackend;
import com.waridley.ttv.logger.backend.mongo.codecs.CredentialCodecProvider;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;

public class Launcher {
	
	protected static String redirectUrl = "http://localhost:6464";
	protected static String dbname = "chatgame";
	protected static String channelName;
	protected static String clientId;
	protected static String clientSecret;
	protected static RefreshingProvider idProvider;
	protected static MongoDatabase db;
	protected static IStorageBackend credBackend;
	protected static CredentialManager credentialManager;
	protected static TwitchClient twitchClient;
//	protected static TMIHostGetter tmiHostGetter;
	protected static TtvStorageInterface ttvBackend;
	protected static long intervalMinutes = 6L;
	
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
		clientId = args[1];
		clientSecret = args[2];
		idProvider = new RefreshingProvider(clientId, clientSecret, redirectUrl);
		db = connectToDatabase(args[3]);
		dbname = args[4];
		
		MongoCollection<Document> credCollection = MongoBackend.createCollectionIfNotExists(db, "credentials", Document.class)
				.withCodecRegistry(CodecRegistries.fromRegistries(
							com.mongodb.MongoClient.getDefaultCodecRegistry(),
							CodecRegistries.fromProviders(new CredentialCodecProvider())
						)
				);
		
		credBackend = new NamedCredentialStorageBackend(new MongoMap<>(
				credCollection,
				Credential.class));
		
		DesktopAuthController authController = new DesktopAuthController(redirectUrl + "/info.html");
		
		credentialManager = CredentialManagerBuilder.builder()
				.withAuthenticationController(authController)
				.withStorageBackend(credBackend)
				.build();
		credentialManager.registerIdentityProvider(idProvider);
		
		NamedCredentialLoader namedCredentialLoader = new NamedCredentialLoader(idProvider, Launcher::onReceivedCredential);
		namedCredentialLoader.startCredentialRetrieval("loggerCredential");
		
	}
	
	private synchronized static void onReceivedCredential(OAuth2Credential credential) {
		Optional<OAuth2Credential> enrichedCred = idProvider.getAdditionalCredentialInformation(credential);
		if(enrichedCred.isPresent()) {
			credential = enrichedCred.get();
			System.out.println("Retrieved chat credential for: " + credential.getUserName());
		}
		((NamedCredentialStorageBackend) credBackend).storeNamedCredential("loggerCredential", credential);
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
		return mongoClient.getDatabase(dbname);
	}
	
	private synchronized static LoggerOptions createOptionsFromArgs(String[] args) {
		return new LoggerOptionsBuilder()
				.withStorageInterface(ttvBackend)
				.build();
	}
	
	
}