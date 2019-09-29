/**
 * Copyright (c) 2019 Kevin Day
 * Licensed under the EUPL
 */

package com.waridley.ttv.logger.backend.mongo;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.waridley.ttv.TtvUser;
import com.waridley.ttv.TtvStorageInterface;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.*;

public class MongoTtvBackend implements TtvStorageInterface, MongoBackend {
	
	private final MongoDatabase db;
	@Override
	public MongoDatabase db() { return db; }
	
	private final MongoCollection<TtvUser> ttvUserCollection;
	
	private TwitchHelix helix;
	@Override
	public TwitchHelix helix() {
		return helix;
	}
	
	private OAuth2Credential helixCredential;
	@Override
	public String helixAccessToken() {
		return helixCredential != null ? helixCredential.getAccessToken() : null;
	}
	
	private Map<Long, TtvUser> ttvUserCache = Collections.synchronizedSortedMap(new TreeMap<>());
	
	public MongoTtvBackend( MongoDatabase db, TwitchHelix helix, OAuth2Credential helixCredential) {
		this.db = db;
		this.helix = helix;
		this.helixCredential = helixCredential;
		
		List<Convention> conventions = new ArrayList<>(Conventions.DEFAULT_CONVENTIONS);
		conventions.add(Conventions.SET_PRIVATE_FIELDS_CONVENTION);
		PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder()
				.automatic(false)
				.conventions(conventions)
				.register(User.class)
				.register(TtvUser.class)
				.build();
		CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				com.mongodb.MongoClient.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(pojoCodecProvider)
		);
		
		
		ttvUserCollection = createCollectionIfNotExists("ttv_users", TtvUser.class).withCodecRegistry(codecRegistry);
//
	}
	
	//region Moved Credential load and save methods to CredentialBackend
	//	@Override
//	@Deprecated
//	public Optional<NamedOAuth2Credential> loadNamedCredential(String name) {
//		return Optional.ofNullable(adminCollection.find(Filters.eq("name", name)).first());
//	}
//
//	@Override
//	@Deprecated
//	public void saveNamedCredential(String name, OAuth2Credential credential) {
//		//OAuth2Codec storableCred = new OAuth2Codec(credential);
//		adminCollection.findOneAndUpdate(
//				Filters.eq("name", name),
//				new Document("$set", new Document("credential", credential)),
//				new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
//		);
//	}
//
//	@Override
//	public IStorageBackend getCredentialStorageBackend() {
//		return credStorageBackend;
//	}
	//endregion
	

	//region Deprecated TwitchUser methods
	//region findOrCreateTwitchUser
//	public TwitchUser findOrCreateTwitchUser(long ttvUserId) throws TwitchUser.UserNotFoundException {
//		UserList chatters = helix.getUsers(
//				null,
//				Collections.singletonList(ttvUserId),
//				null
//		).execute();
//		try {
//			return findOrCreateTwitchUser(chatters.getUsers().get(0));
//		} catch(IndexOutOfBoundsException e) {
//			throw new TwitchUser.UserNotFoundException("Couldn't find helix user", e);
//		}
//	}
//
//	public TwitchUser findOrCreateTwitchUser(String username) throws TwitchUser.UserNotFoundException {
//		UserList chatters = helix.getUsers(
//				null,
//				null,
//				Collections.singletonList(username.toLowerCase())
//		).execute();
//		try {
//			return findOrCreateTwitchUser(chatters.getUsers().get(0));
//		} catch(IndexOutOfBoundsException e) {
//			throw new TwitchUser.UserNotFoundException("Couldn't find helix user", e);
//		}
//	}
//
//	public TwitchUser findOrCreateTwitchUser(User user) {
//
//		TwitchUser twitchUser = twitchUserCollection.findOneAndUpdate(
//				Filters.eq("userid", user.getId()),
//				Updates.combine(
//						new Document(
//								"$setOnInsert",
//								new Document("userid", user.getId())
//										.append("onlineMinutes", 0L)
//										.append("offlineMinutes", 0L)
//						),
//						new Document(
//								"$set",
//								new Document("login", user.getLogin())
//										.append("helixUser", user)
//						)
//				),
//				new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
//		);
//
//		/*assert twitchUser != null;
//		if(twitchUser.getPlayerId() == null) {
//			System.out.println("Creating player for Twitch user " + twitchUser.getHelixUser().getDisplayName());
//			Player player = new Player(twitchUser.getUserId());
//			System.out.println("Created player " + player.getUsername());
//			//TODO: Save player to GameStorageInterface
//			//twitchUser.setPlayerId(player.getObjectId());
//			twitchUserCollection.updateOne(
//					Filters.eq("userid", twitchUser.getUserId()),
//					new Document("$set", new Document("playerId", twitchUser.getPlayerId())),
//					new UpdateOptions()
//			);
//		}*/
//		return twitchUser;
//	}
	//endregion

	//region findTwitchUser()
//	public TwitchUser findTwitchUser(long ttvUserId) throws TwitchUser.UserNotFoundException {
//		UserList chatters = helix.getUsers(
//				null,
//				Collections.singletonList(ttvUserId),
//				null
//		).execute();
//		try {
//			return findOrCreateTwitchUser(chatters.getUsers().get(0));
//		} catch(IndexOutOfBoundsException e) {
//			throw new TwitchUser.UserNotFoundException("Couldn't find helix user", e);
//		}
//	}
//
//	public TwitchUser findTwitchUser(String username) throws TwitchUser.UserNotFoundException {
//		UserList chatters = helix.getUsers(
//				null,
//				null,
//				Collections.singletonList(username.toLowerCase())
//		).execute();
//		for(User user : chatters.getUsers()) {
//			System.out.println("Found Helix user: " + user.getDisplayName());
//			return findTwitchUser(user);
//		}
//		throw new TwitchUser.UserNotFoundException("Couldn't find Helix user for " + username);
//	}
//
//	public TwitchUser findTwitchUser(User user) throws TwitchUser.UserNotFoundException {
//		TwitchUser result = null;
//		FindIterable<TwitchUser> userIterable = twitchUserCollection.find(Filters.eq("userid", user.getId()));
//		for(TwitchUser twitchUser : userIterable) {
//			if(result == null) result = twitchUser;
//			else throw new TwitchUser.UserNotFoundException("More than one user found for id: " + user.getId());
//		}
//		if(result == null) throw new TwitchUser.UserNotFoundException("User not found for id: " + user.getId());
//		return result;
//	}
	//endregion
//
//	public TwitchUser logMinutes(TwitchUser user, long minutes, boolean online) {
//		String status;
//		long currentMinutes;
//
//		if(online) {
//			status = "online";
//			currentMinutes = user.getOnlineMinutes();
//		} else {
//			status = "offline";
//			currentMinutes = user.getOfflineMinutes();
//		}
//
//		TwitchUser updatedUser = twitchUserCollection.findOneAndUpdate(
//				Filters.eq("userid", user.getUserId()),
//				new Document("$set", new Document()
//						.append(status + "Minutes", currentMinutes + minutes)),
//				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
//		);
//
//		//System.out.println("Logged " + minutes + " minutes for " + updatedUser.getHelixUser().getDisplayName());
//
//		return updatedUser;
//	}
	//endregion
	
	
	//region New TtvUser methods
	//region findOrCreateTtvUser()
	@Override
	public TtvUser findOrCreateTtvUser(long ttvUserId) {
		TtvUser ttvUser = ttvUserCache.get(ttvUserId);
		if(ttvUser == null) {
			UserList chatters = helix.getUsers(
					helixAccessToken(),
					Collections.singletonList(ttvUserId),
					null
			).execute();
			for(User u : chatters.getUsers()) {
				ttvUser = findOrCreateTtvUser(u);
			}
		}
		return ttvUser;
	}
	
	@Override
	public TtvUser findOrCreateTtvUser(String ttvLogin) {
		TtvUser ttvUser = null;
		for(TtvUser u : ttvUserCache.values()) {
			if(u.getHelixUser().getLogin().equalsIgnoreCase(ttvLogin)) {
				ttvUser = u;
				break;
			}
		}
		if(ttvUser == null) {
			UserList chatters = helix.getUsers(
					null,
					null,
					Collections.singletonList(ttvLogin)
			).execute();
			for(User u : chatters.getUsers()) {
				ttvUser = findOrCreateTtvUser(u);
			}
		}
		return ttvUser;
	}
	
	@Override
	public TtvUser findOrCreateTtvUser(User user) {
		TtvUser ttvUser = ttvUserCollection.findOneAndUpdate(
				Filters.eq("_id", user.getId()),
				Updates.combine(
						Updates.setOnInsert(
								new Document("_id", user.getId())
										.append("onlineMinutes", 0L)
										.append("offlineMinutes", 0L)
										.append("guestMinutes", 0L)
						),
						new Document(
								"$set",
								new Document("helixUser", user)
						)
				),
				new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
		);
		if(ttvUser != null) ttvUserCache.put(ttvUser.getId(), ttvUser);
		return ttvUser;
	}
	
	//endregion
	
	//region findTtvUser()
	
	//made default
//	@Override
//	public Optional<TtvUser> findTtvUser(long ttvUserId) {
//		Optional<TtvUser> result = Optional.ofNullable(ttvUserCache.get(ttvUserId));
//		if(!result.isPresent()) {
//			UserList chatters = helix.getUsers(
//					null,
//					Collections.singletonList(ttvUserId),
//					null
//			).execute();
//			for(User u : chatters.getUsers()) {
//				result = findTtvUser(u);
//			}
//		}
//		return result;
//	}
//
	//made default
//	@Override
//	public Optional<TtvUser> findTtvUser(String ttvLogin) {
//		Optional<TtvUser> result = Optional.empty();
//		for(TtvUser u : ttvUserCache.values()) {
//			if(u.getHelixUser().getLogin().equalsIgnoreCase(ttvLogin)) {
//				result = Optional.of(u);
//			}
//		}
//		if(!result.isPresent()) {
//			List<User> userList = getHelixUsers(null, Collections.singletonList(ttvLogin));
//			for(User u : userList) {
//				result = findTtvUser(u);
//			}
//		}
//		return result;
//	}
	
	@Override
	public Optional<TtvUser> findTtvUser(User user) {
		TtvUser result = ttvUserCollection.find(Filters.eq("_id", user.getId())).first();
		if(result != null) ttvUserCache.put(result.getId(), result);
		return Optional.ofNullable(result);
	}
	
	public List<TtvUser> findTtvUsers(List<User> helixUsers) {
		List<Long> userIds = new ArrayList<>(helixUsers.size());
		for(User u : helixUsers) {
			userIds.add(u.getId());
		}
		return findTtvUsersByIds(userIds);
	}
	
	@Override
	public List<TtvUser> findTtvUsersByIds(List<Long> userIds) {
		List<TtvUser> result = new ArrayList<>(userIds.size());
		for(TtvUser u : ttvUserCollection.find(Filters.in("_id", userIds))) {
			result.add(u);
			ttvUserCache.put(u.getId(), u);
		}
		return result;
	}
	
//	@Override
//	public List<User> getHelixUsers(List<Long> ids, List<String> logins) {
//		Set<User> resultSet = new HashSet<>();
//
//		List<Long> uncachedIds = new ArrayList<>();
//		List<String> uncachedLogins = new ArrayList<>();
//
//		for(long id : ids) {
//			TtvUser ttvUser = ttvUserCache.get(id);
//			if(ttvUser != null) resultSet.add(ttvUser.getHelixUser());
//			else uncachedIds.add(id);
//		}
//		for(String login : logins) {
//			for(TtvUser user : ttvUserCache.values()) {
//				if(user.getHelixUser() != null && user.getHelixUser().getLogin().equalsIgnoreCase(login)) resultSet.add(user.getHelixUser());
//				else uncachedLogins.add(login);
//			}
//		}
//
//		List<User> userList = helix.getUsers(
//				null,
//				uncachedIds,
//				uncachedLogins
//		).execute().getUsers();
//		resultSet.addAll(userList);
//
//		return new ArrayList<>(resultSet);
//	}
	
	//endregion
	
	public void saveTtvUser(TtvUser user) {
		ttvUserCollection.findOneAndUpdate(
				Filters.eq("_id", user.getId()),
				new Document("$set",
						new Document("offlineMinutes", user.getOfflineMinutes())
							.append("onlineMinutes", user.getOnlineMinutes())
							.append("guestMinutes", user.getGuestMinutes())
							.append("helixUser", user.getHelixUser())
				),
				new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
		);
	}
	
	@Override
	public TtvUser logMinutes(TtvUser user, long minutes, boolean online) {
		String status;
		long currentMinutes;
		
		if(online) {
			status = "online";
			currentMinutes = user.getOnlineMinutes();
		} else {
			status = "offline";
			currentMinutes = user.getOfflineMinutes();
		}
		
		TtvUser updatedUser = ttvUserCollection.findOneAndUpdate(
				Filters.eq("_id", user.getId()),
				new Document("$set", new Document()
						.append(status + "Minutes", currentMinutes + minutes)),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
		);
		
		ttvUserCache.put(updatedUser.getId(), updatedUser);
		
		//System.out.println("Logged " + minutes + " minutes for " + updatedUser.getHelixUser().getDisplayName());
		
		return updatedUser;
	}
	
	@Override
	public TtvUser logGuestMinutes(TtvUser user, long minutes, String guestLogin) {
		long currentMinutes = user.getGuestMinutes();
		//System.out.println("Logging " + minutes + " minutes for " + user.getHelixUser().getDisplayName());
		//System.out.println(user.getHelixUser().getDisplayName() + " currently has " + user.getGuestMinutes() + " guest minutes");
		TtvUser updatedUser = ttvUserCollection.findOneAndUpdate(
				Filters.eq("_id", user.getId()),
				new Document("$set", new Document()
						.append("guestMinutes", currentMinutes + minutes)),
				new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
		);
		
		ttvUserCache.put(updatedUser.getId(), updatedUser);
		
		//System.out.println("Logged " + minutes + " guest minutes for " + updatedUser.getHelixUser().getDisplayName());
		
		return updatedUser;
	}
	//endregion
	
}
