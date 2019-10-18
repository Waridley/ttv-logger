package com.waridley.ttv.logger;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.credentialmanager.identityprovider.OAuth2IdentityProvider;
import com.github.twitch4j.auth.domain.TwitchScopes;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.waridley.credentials.NamedCredentialStorageBackend;
import com.waridley.ttv.RefreshingProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public class NamedCredentialLoader {
	
	private CredentialConsumer consumer;
	
	private NamedCredentialStorageBackend credentialStorage;
	
	private CredentialManager credentialManager;
	private OAuth2IdentityProvider identityProvider;
	
	
	public NamedCredentialLoader(OAuth2IdentityProvider provider, CredentialConsumer consumer) {
		
		this.consumer = consumer;
		this.credentialManager = provider.getCredentialManager();
		this.credentialStorage = (NamedCredentialStorageBackend) provider.getCredentialManager().getStorageBackend();
		this.identityProvider = provider;
		
	}
	
	
	public void startCredentialRetrieval(String name) throws IOException {
		Optional<Credential> botCredOpt = credentialStorage.getCredentialByName(name);
		
		if(botCredOpt.isPresent() && botCredOpt.get() instanceof OAuth2Credential) {
			System.out.println("Found bot credential.");
			OAuth2Credential credential = (OAuth2Credential) botCredOpt.get();
			Optional<OAuth2Credential> refreshedCredOpt = ((RefreshingProvider) identityProvider).refreshCredential(credential);
			if(refreshedCredOpt.isPresent()) {
				credential = refreshedCredOpt.get();
				System.out.println("Successfully refreshed token");
			}
			consumer.consumeCredential(credential);
		} else {
			System.out.println("No saved credential found named " + name + ". Starting OAuth2 Authorization Code Flow.");
			
			HttpServer server = HttpServer.create(new InetSocketAddress(6464), 0);
			server.createContext("/", this::onReceivedCode);
			server.createContext("/info.html", this::handleInfoPage);
			server.start();
			
			
			credentialManager.getAuthenticationController().startOAuth2AuthorizationCodeGrantType(
					identityProvider,
					null,
					Arrays.asList(
							TwitchScopes.CHAT_CHANNEL_MODERATE,
							TwitchScopes.CHAT_EDIT,
							TwitchScopes.CHAT_READ,
							TwitchScopes.CHAT_WHISPERS_EDIT,
							TwitchScopes.CHAT_WHISPERS_READ
					));
		}
	}
	
	private void handleInfoPage(HttpExchange exchange) throws IOException {
		
		String authUrl = "";
		URI reqURI = exchange.getRequestURI();
		String[] queryParams = reqURI.getQuery().split("&");
		for(String param : queryParams) {
			if(param.startsWith("authurl=")) {
				authUrl = param.replaceFirst("authurl=", "");
				authUrl = URLDecoder.decode(authUrl, StandardCharsets.UTF_8.toString());
			}
		}
		String response =
				"<html>" +
						"<head>" +
						"</head>" +
						"<body>" +
						"<h1>Log in to your desired chat bot account</h1>" +
						"The following link will take you to the Twitch authentication page to log in.<br>" +
						"If you do not want to use your main account for the chat bot, you can either:<br>" +
						"<p style=\"margin-left: 40px\">1) Click \"Not you?\" on that page, however, this will permanently change the account you are logged into on Twitch until you manually switch back.</p>" +
						"<p style=\"margin-left: 40px\">2) Right-click this link, and open it in a private/incognito window. This will allow you to stay logged in to Twitch on your main account in normal browser windows.</p>" +
						"<a href=" + authUrl + ">" + authUrl + "</a>" +
						"</body>" +
						"</html>";
		exchange.sendResponseHeaders(200, response.length());
		exchange.getResponseBody().write(response.getBytes());
		exchange.getResponseBody().close();
	}
	
	private void onReceivedCode(HttpExchange exchange) {
		if(true) { //TODO check if response was successful
			
			String code = null;
			
			URI uri = exchange.getRequestURI();
			String response =
					"<html>" +
						"<head>" +
						"</head>" +
						"<body>" +
							"<h1>Success!</h1>" +
							"Received authorization code. Getting token and joining chat." +
						"</body>" +
					"</html>";
			try {
				exchange.sendResponseHeaders(200, response.length());
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			String query = uri.getQuery();
			String[] splitQuery = query.split("&");
			for(String s : splitQuery) {
				String[] splitField = s.split("=");
				if(splitField[0].equals("code")) {
					code = splitField[1];
					break;
				}
			}
			OAuth2Credential cred = identityProvider.getCredentialByCode(code);
			identityProvider.getCredentialManager().addCredential("twitch", cred);
			consumer.consumeCredential(cred);
		} else {
			String response =
					"<html>" +
						"<head>" +
						"</head>" +
						"<body>" +
							"<h1>Oops!</h1>" +
							"Something went wrong. Can't retrieve token." +
						"</body>" +
					"</html>";
			try {
				exchange.sendResponseHeaders(200, response.length());
				exchange.getResponseBody().write(response.getBytes());
				exchange.getResponseBody().close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	interface CredentialConsumer {
		void consumeCredential(OAuth2Credential credential);
	}
	
}
