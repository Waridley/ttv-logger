/**
 * Copyright (c) 2019 Kevin Day
 * Licensed under the EUPL
 */

package com.waridley.ttv;

import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface TtvStorageInterface {
	
	TwitchHelix helix();
	String helixAccessToken();
	
	default TtvUser findOrCreateTtvUser(long ttvUserId) {
		return findOrCreateTtvUser(getUsersFromIds(Collections.singletonList(ttvUserId)).get(0));
	}
	default TtvUser findOrCreateTtvUser(String ttvLogin) {
		return findOrCreateTtvUser(getHelixUsersFromLogins(Collections.singletonList(ttvLogin)).get(0));
	}
	TtvUser findOrCreateTtvUser(User user);
	
	default Optional<TtvUser> findTtvUser(Long ttvUserId) {
		return findTtvUser(getUsersFromIds(Collections.singletonList(ttvUserId)).get(0));
	}
	default Optional<TtvUser> findTtvUser(String ttvLogin) {
		return findTtvUser(getHelixUsersFromLogins(Collections.singletonList(ttvLogin)).get(0));
	}
	Optional<TtvUser> findTtvUser(User user);
	
	List<TtvUser> findTtvUsers(List<User> helixUsers);
	List<TtvUser> findTtvUsersByIds(List<Long> userIds);
	
	
	default List<User> getUsersFromIds(List<Long> ids) {
		List<User> result = new ArrayList<>(ids.size());
		int divSize = 100;
		List<List<Long>> idLists = new ArrayList<>((ids.size() / divSize) + 1);
		for(int i = 0; i < ids.size(); i += divSize) {
			int to = i + divSize;
			if(to >= ids.size()) to = ids.size();
			idLists.add(ids.subList(i, to));
		}
		for(List<Long> l : idLists) {
			UserList userList = helix().getUsers(
					helixAccessToken(),
					l,
					null
			).execute();
			result.addAll(userList.getUsers());
		}
		return result;
	}
	
	default List<User> getHelixUsersFromLogins(List<String> logins) {
		List<User> result = new ArrayList<>(logins.size());
		int divSize = 100;
		List<List<String>> loginLists = new ArrayList<>((logins.size() / divSize) + 1);
		for(int i = 0; i < logins.size(); i += divSize) {
			int to = i + divSize;
			if(to >= logins.size()) to = logins.size();
			loginLists.add(logins.subList(i, to));
		}
		for(List<String> l : loginLists) {
			UserList userList = helix().getUsers(
					helixAccessToken(),
					null,
					l
			).execute();
			result.addAll(userList.getUsers());
		}
		
		return result;
	}
	
	TtvUser logMinutes(TtvUser user, long minutes, boolean online);
	TtvUser logGuestMinutes(TtvUser user, long minutes, String guestName);
}
