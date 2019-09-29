package com.waridley.ttv;

import com.github.twitch4j.common.builder.TwitchAPIBuilder;
import com.github.twitch4j.common.feign.interceptor.TwitchClientIdInterceptor;
import com.github.twitch4j.tmi.TwitchMessagingInterfaceErrorDecoder;
import com.netflix.config.ConfigurationManager;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Twitch API - Messaging Interface
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TMIHostGetterBuilder extends TwitchAPIBuilder<com.waridley.ttv.TMIHostGetterBuilder> {
	
	/**
	 * BaseUrl
	 */
	private String baseUrl = "https://tmi.twitch.tv";
	
	
	
	/**
	 * Initialize the builder
	 *
	 * @return Twitch Helix Builder
	 */
	public static TMIHostGetterBuilder builder() {
		return new TMIHostGetterBuilder();
	}
	
	/**
	 * Twitch API Client (Helix)
	 *
	 * @return TwitchHelix
	 */
	public TMIHostGetter build() {
		//log.debug("TMI: Initializing Module ...");
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 2500);
		TMIHostGetter client = HystrixFeign.builder()
				.encoder(new JacksonEncoder())
				.decoder(new JacksonDecoder())
				.logger(new Logger.ErrorLogger())
				.errorDecoder(new TwitchMessagingInterfaceErrorDecoder(new JacksonDecoder()))
				.logLevel(Logger.Level.BASIC)
				.requestInterceptor(new TwitchClientIdInterceptor(this))
				.retryer(new Retryer.Default(1, 10000, 3))
				.options(new Request.Options(5000, 15000))
				.target(TMIHostGetter.class, baseUrl);
		
		// register with serviceMediator
		getEventManager().getServiceMediator().addService("waridley-tmi-hostgetter", client);
		
		return client;
	}
}
