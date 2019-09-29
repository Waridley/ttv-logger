package com.waridley.ttv.logger;

import com.waridley.ttv.TtvStorageInterface;

public class LoggerOptionsBuilder {
	
	private TtvStorageInterface storageInterface;
	private String channelName;
	
	public LoggerOptionsBuilder forChannel(String channelName) {
		this.channelName = channelName;
		return this;
	}
	
	public LoggerOptionsBuilder withStorageInterface(TtvStorageInterface storageInterface) {
		this.storageInterface = storageInterface;
		return this;
	}
	
	public LoggerOptions build() {
		return new LoggerOptions(channelName, storageInterface);
	}
	
}
