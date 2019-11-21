package com.waridley.ttv.logger

import com.github.philippheuer.credentialmanager.CredentialManager
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.credentialmanager.identityprovider.OAuth2IdentityProvider
import com.github.twitch4j.auth.domain.TwitchScopes
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.waridley.credentials.NamedCredentialStorageBackend
import com.waridley.ttv.RefreshingProvider
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class NamedCredentialLoader @JvmOverloads constructor(provider: OAuth2IdentityProvider, private val consumer: CredentialConsumer, private val infoPath: String? = null) {
	private val credentialStorage: NamedCredentialStorageBackend = provider.credentialManager.storageBackend as NamedCredentialStorageBackend
	private val credentialManager: CredentialManager = provider.credentialManager
	private val identityProvider: OAuth2IdentityProvider = provider
	private lateinit var server: HttpServer
	
	@Throws(IOException::class)
	fun startCredentialRetrieval(name: String) {
		val credOpt = credentialStorage.getCredentialByName(name)
		if (credOpt.isPresent && credOpt.get() is OAuth2Credential) {
			log.info("Found credential named {}", name)
			var credential = credOpt.get() as OAuth2Credential
			val refreshedCredOpt = (identityProvider as RefreshingProvider).refreshCredential(credential)
			if (refreshedCredOpt.isPresent) {
				credential = refreshedCredOpt.get()
				log.info("Successfully refreshed token")
			}
			consumer.consumeCredential(credential)
		} else {
			log.info("No saved credential found named {}. Starting OAuth2 Authorization Code Flow.", name)
			server = HttpServer.create(InetSocketAddress(6464), 0)
			server.createContext("/") { exchange: HttpExchange -> onReceivedCode(exchange) }
			if (infoPath != null) server.createContext(infoPath) { exchange: HttpExchange -> handleInfoPage(exchange) }
			server.start()
			credentialManager.authenticationController.startOAuth2AuthorizationCodeGrantType(
					identityProvider,
					null,
					listOf<Any>(
							TwitchScopes.CHAT_CHANNEL_MODERATE,
							TwitchScopes.CHAT_EDIT,
							TwitchScopes.CHAT_READ,
							TwitchScopes.CHAT_WHISPERS_EDIT,
							TwitchScopes.CHAT_WHISPERS_READ
					))
		}
	}
	
	@Throws(IOException::class)
	private fun handleInfoPage(exchange: HttpExchange) {
		var authUrl = ""
		val reqURI = exchange.requestURI
		val queryParams = reqURI.query.split("&").toTypedArray()
		for (param in queryParams) {
			if (param.startsWith("authurl=")) {
				authUrl = param.replaceFirst("authurl=".toRegex(), "")
				authUrl = URLDecoder.decode(authUrl, StandardCharsets.UTF_8.toString())
			}
		}
		val response = "<html>" +
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
				"</html>"
		exchange.sendResponseHeaders(200, response.length.toLong())
		exchange.responseBody.write(response.toByteArray())
		exchange.responseBody.close()
	}
	
	private fun onReceivedCode(exchange: HttpExchange) {
		if (true) { //TODO check if response was successful
			var code: String? = null
			val uri = exchange.requestURI
			val response = """<html><head></head><body><h1>Success!</h1>Received authorization code. Getting token and joining chat.</body></html>"""
			try {
				exchange.sendResponseHeaders(200, response.length.toLong())
				exchange.responseBody.write(response.toByteArray())
				exchange.responseBody.close()
			} catch (e: IOException) {
				e.printStackTrace()
			}
			val query = uri.query
			val splitQuery = query.split("&").toTypedArray()
			for (s in splitQuery) {
				val splitField = s.split("=").toTypedArray()
				if (splitField[0] == "code") {
					code = splitField[1]
					break
				}
			}
			val cred = identityProvider.getCredentialByCode(code)
			identityProvider.credentialManager.addCredential("twitch", cred)
			consumer.consumeCredential(cred)
		} else {
			val response = """<html><head></head><body><h1>Oops!</h1>Something went wrong. Can't retrieve token.</body></html>"""
			try {
				exchange.sendResponseHeaders(200, response.length.toLong())
				exchange.responseBody.write(response.toByteArray())
				exchange.responseBody.close()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		server.stop(0)
	}
	
	interface CredentialConsumer {
		fun consumeCredential(credential: OAuth2Credential?)
	}
	
	companion object {
		private val log = LoggerFactory.getLogger(NamedCredentialLoader::class.java)
	}
	
}