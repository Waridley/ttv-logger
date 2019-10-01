package com.waridley.ttv;

import com.github.twitch4j.tmi.TwitchMessagingInterface;
import com.netflix.hystrix.HystrixCommand;
import feign.Param;
import feign.RequestLine;

import java.util.List;

/**
 * Twitch - Messaging Interface
 */
public interface TMIHostGetter extends TwitchMessagingInterface {
	
	@RequestLine("GET /hosts?include_logins=1&host={id}")
	HystrixCommand<HostList> getHosts(
			@Param("id") List<String> hostIds
	);
	
	@RequestLine("GET /hosts?include_logins=1&target={id}")
	HystrixCommand<HostList> getHostsOf(
			@Param("id") String targetIds
	);
}
