package com.waridley.ttv.logger;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.EventManager;
import com.github.philippheuer.events4j.domain.Event;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.TwitchChat;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.waridley.credentials.DesktopAuthController;
import com.waridley.credentials.NamedCredentialStorageBackend;
import com.waridley.credentials.mongo.MongoCredentialStorageBackend;
import com.waridley.ttv.RefreshingProvider;
import com.waridley.ttv.TtvStorageInterface;
import com.waridley.ttv.logger.backend.local.FileMessageLogger;
import com.waridley.ttv.logger.backend.mongo.MongoMessageLogger;
import com.waridley.ttv.mongo.MongoTtvBackend;
import org.slf4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.WaitStrategy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

@Command(name = "ttv-logger")
public class Launcher implements Runnable {
	
	private Logger log;
	
	@Parameters(index = "0", arity="1")           private String channelName = null;
	
	@Option(names = {"-r", "--redirectUrl"})      private String redirectUrl = "http://localhost";
	@Option(names = {"-p", "--infoPath"})         private String infoPath = null;
	@Option(names = {"-m", "--connectionString"}) private String connectionString = null;
	@Option(names = {"-d", "--DB", "--dbName"})   private String dbname = "ttvlogger";
	@Option(names = {"-a", "--token"})            private String oauth2Token = null;
	@Option(names = {"-t", "--logInterval"})      private long intervalMinutes = 0L;
	@Option(names = {"-c", "--chatCollection"})   private String chatCollectionName = null;
	@Option(names = {"-f", "--chatLogFile"})      private String chatLogFilePath = null;
	@Option(names = {"--printChat"})              private boolean printChat;
	
	@Option(names = "--logLevels", split = ",")
	private Map<String, String> logLevels = new HashMap<>();
	
	@Option(names = {"-i", "--clientId"})
	private String clientId = "yod6n4not2ypylyszb7rtuqnpaq6q3"; //Waridley's TtvLogger ClientID
	
	@Option(names = {"-s", "--clientSecret"}, arity = "0..1", interactive = true)
	private String clientSecret = null;
	
	@Option(names = "-v", description = {
			"Specify multiple -v options to increase verbosity.",
			"For example, `-v -v -v` or `-vvv`"})
	private boolean[] v = new boolean[0];
	
	private RefreshingProvider idProvider;
	private MongoClient mongoClient;
	private MongoDatabase db;
	private NamedCredentialStorageBackend credBackend;
	private CredentialManager credentialManager;
	
	
	public static void main(String... args) {
		new CommandLine(new Launcher(args)).execute(args);
	}
	
	public Launcher(String... args) {
		
		Properties props = System.getProperties();
//		props.put("org.slf4j.simpleLogger.defaultLogLevel", "trace");
		props.put("org.slf4j.simpleLogger.showThreadName", "false");
		props.put("org.slf4j.simpleLogger.showLogName", "false");
		props.put("org.slf4j.simpleLogger.showShortLogName", "true");
		logLevels.put("org.slf4j.simpleLogger.log." + Launcher.class.getName(), "info");
		logLevels.put("org.slf4j.simpleLogger.log." + ChatLogger.class.getName(), "info");
		logLevels.put("org.slf4j.simpleLogger.log." + WatchtimeLogger.class.getName(), "info");
		
		for(String arg : args) {
			if(v.length > 0) {
				if(v.length == 3) {
					props.put("org.slf4j.simpleLogger.defaultLogLevel", "trace");
					logLevels.put("org.slf4j.simpleLogger.log." + ChatLogger.class.getName(), "trace");
					logLevels.put("org.slf4j.simpleLogger.log." + WatchtimeLogger.class.getName(), "trace");
				} else {
					props.put("org.slf4j.simpleLogger.log." + ChatLogger.class.getName(), "debug");
					props.put("org.slf4j.simpleLogger.log." + WatchtimeLogger.class.getName(), "debug");
					if(v.length == 2) {
						props.put("org.slf4j.simpleLogger.defaultLogLevel", "debug");
					}
				}
			} else if(arg.startsWith("--chatLogCollection=")) {
				chatCollectionName = arg.substring(20);
			} else if(arg.startsWith("--chatLogFile=")) {
				chatLogFilePath = arg.substring(14);
			}
		}
		
		for(Map.Entry<String, String> level: logLevels.entrySet()) {
			System.getProperties().put("org.slf4j.simpleLogger.log." + level.getKey(), level.getValue());
		}
		
		log = org.slf4j.LoggerFactory.getLogger(Launcher.class);
		
	}
	
	
	private synchronized void init() throws URISyntaxException, IOException {
		
		idProvider = new RefreshingProvider(clientId, clientSecret, redirectUrl);
		db = connectToDatabase();
		
		credBackend = new MongoCredentialStorageBackend(db, "credentials");
		
		DesktopAuthController authController;
		if(infoPath != null) authController = new DesktopAuthController(redirectUrl + infoPath);
		else authController = new DesktopAuthController();
		
		credentialManager = CredentialManagerBuilder.builder()
				.withAuthenticationController(authController)
				.withStorageBackend(credBackend)
				.build();
		credentialManager.registerIdentityProvider(idProvider);
		
		NamedCredentialLoader namedCredentialLoader = new NamedCredentialLoader(idProvider, this::onReceivedCredential, infoPath);
		namedCredentialLoader.startCredentialRetrieval("loggerCredential");
		
	}
	
	private synchronized void onReceivedCredential(OAuth2Credential credential) {
		Optional<OAuth2Credential> enrichedCred = idProvider.getAdditionalCredentialInformation(credential);
		if(enrichedCred.isPresent()) {
			credential = enrichedCred.get();
			log.info("Retrieved chat credential for: " + credential.getUserName());
		}
		credBackend.saveCredential("loggerCredential", credential);
		
		EventManager eventManager = new EventManager(
				Schedulers.newParallel("events4j-scheduler"),
				TopicProcessor.<Event>builder()
						.name("events4j-processor")
						.waitStrategy(WaitStrategy.sleeping())
						.bufferSize(16384)
						.build(),
				FluxSink.OverflowStrategy.BUFFER);
		
		TwitchClient twitchClient = TwitchClientBuilder.builder()
				.withEventManager(eventManager)
				.withEnableHelix(true)
				.withEnableTMI(true)
				.withEnableChat(true)
				.withChatAccount(credential)
				.withClientId(clientId)
				.withClientSecret(clientSecret)
				.withRedirectUrl(redirectUrl)
				.withCredentialManager(credentialManager)
				.build();
		twitchClient.getClientHelper().enableStreamEventListener(channelName);
		
		TtvStorageInterface ttvBackend = new MongoTtvBackend(db, twitchClient.getHelix(), credential);
		
//		TwitchChat chat = new TwitchChat(twitchClient.getEventManager(), credentialManager, credential, true, Collections.emptyList());
		TwitchChat chat = twitchClient.getChat();
		
		ChatLogger chatLogger = new ChatLogger(chat, channelName);
		
		if(chatCollectionName != null) {
			log.debug("Creating MongoDB chat logger: {}", chatCollectionName);
			chatLogger.addMessageLogger(new MongoMessageLogger(
					db,
					chatCollectionName));
		}
		if(chatLogFilePath != null) {
			try {
				log.debug("Creating FileChatLogger: {}", chatLogFilePath);
				chatLogger.addMessageLogger(new FileMessageLogger(
						chatLogFilePath));
			} catch(IOException e) {
				log.error("Failed to open chat log file:", e);
			}
		}
		if(printChat) {
			log.info("Will print chat to stdout");
			chatLogger.addMessageLogger(event -> System.out.println(String.format("%s %s... %s", "[" + event.getChannel().getName() + "]", String.format("%-25s", event.getUser().getName()).replace(' ', '.'), event.getMessage())));
		}
		
		if(intervalMinutes > 0L)
			new WatchtimeLogger(ttvBackend, channelName, intervalMinutes).start();
	}
	
	public MongoDatabase connectToDatabase() {
		ConnectionString connStr = new ConnectionString(connectionString);
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(connStr)
				.build();
		
		mongoClient = MongoClients.create(settings);
		return mongoClient.getDatabase(dbname);
	}
	
	@Override
	public void run() {
		try {
			init();
		} catch(URISyntaxException | IOException e) {
			log.error("Failed to initialize: ", e);
		}
	}
}