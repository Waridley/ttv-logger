package com.waridley.ttv;

import com.github.twitch4j.helix.domain.User;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class TtvUser {
	
	private String id;
	
	private User helixUser;
	
	private long offlineMinutes = 0L;
	
	private long onlineMinutes = 0L;
	
	private long guestMinutes = 0L;
	
	private Map<String, Object> properties = new HashMap<>();
	
	private Object getProperty(String name) {
		return properties.get(name);
	}
	
	private void setProperty(String name, Object value) {
		properties.put(name, value);
	}
	
	public TtvUser(User helixUser) {
		setHelixUser(helixUser);
		setId(helixUser.getId());
	}
	
	public long channelMinutes() { return onlineMinutes + offlineMinutes; }
	public long totalMinutes() { return onlineMinutes + offlineMinutes + guestMinutes; }
	
	public static double toHours(long mintues) {
		return ((double) mintues) / 60.0;
	}
}
