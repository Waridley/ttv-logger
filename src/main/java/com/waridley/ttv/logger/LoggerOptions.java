package com.waridley.ttv.logger;

import com.waridley.ttv.TtvStorageInterface;

public class LoggerOptions {
	
	private final String channelName;
	private final TtvStorageInterface storageInterface;
	public TtvStorageInterface getStorageInterface() { return storageInterface; }
	
	
	public LoggerOptions(String channelName, TtvStorageInterface storageInterface) {
		this.channelName = channelName;
		this.storageInterface = storageInterface;
	}
	
	
}
