package com.waridley.ttv.logger.backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import okhttp3.*;

import java.util.HashMap;
import java.util.Map;

public class RefreshingProvider extends TwitchIdentityProvider {
	
	/**
	 * Constructor
	 *
	 * @param clientId     OAuth Client Id
	 * @param clientSecret OAuth Client Secret
	 * @param redirectUrl  Redirect Url
	 */
	public RefreshingProvider(String clientId, String clientSecret, String redirectUrl) {
		super(clientId, clientSecret, redirectUrl);
		scopeSeperator = "+";
	}
	
	/**
	 * Refresh access token using refresh token
	 *
	 * @param oldCredential The credential to refresh
	 * @return The refreshed credential
	 * @throws UnsupportedOperationException If the token endpoint type is not "QUERY" or "BODY", or if the credential has no refresh token.
	 * @throws RuntimeException If the response is unsuccessful
	 */
	public OAuth2Credential refreshCredential(OAuth2Credential oldCredential) {
		// request access token
		OkHttpClient client = new OkHttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			Request request;
			
			if(oldCredential.getRefreshToken() == null) throw new UnsupportedOperationException("Attempting to refresh a credential that has no refresh token.");
			
			if (tokenEndpointPostType.equalsIgnoreCase("QUERY")) {
				HttpUrl.Builder urlBuilder = HttpUrl.parse(this.tokenUrl).newBuilder();
				urlBuilder.addQueryParameter("client_id", this.clientId);
				urlBuilder.addQueryParameter("client_secret", this.clientSecret);
				urlBuilder.addQueryParameter("refresh_token", oldCredential.getRefreshToken());
				urlBuilder.addQueryParameter("grant_type", "refresh_token");
				
				request = new Request.Builder()
						.url(urlBuilder.build().toString())
						.post(RequestBody.create(null, new byte[]{}))
						.build();
			} else if (tokenEndpointPostType.equalsIgnoreCase("BODY")) {
				RequestBody requestBody = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("client_id", this.clientId)
						.addFormDataPart("client_secret", this.clientSecret)
						.addFormDataPart("refresh_token", oldCredential.getRefreshToken())
						.addFormDataPart("grant_type", "refresh_token")
						.build();
				
				request = new Request.Builder()
						.url(this.tokenUrl)
						.post(requestBody)
						.build();
			} else {
				throw new UnsupportedOperationException("Unknown tokenEndpointPostType: " + tokenEndpointPostType);
			}
			
			Response response = client.newCall(request).execute();
			String responseBody = response.body().string();
			if (response.isSuccessful()) {
				Map<String, Object> resultMap = objectMapper.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {});
				
				OAuth2Credential credential = new OAuth2Credential(this.providerName, (String) resultMap.get("access_token"), (String) resultMap.get("refresh_token"), null, null, (Integer) resultMap.get("expires_in"), null);
				return credential;
			} else {
				throw new RuntimeException("getCredentialByCode request failed! " + response.code() + ": " + responseBody);
			}
			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Get a Credential for server-to-server requests using the OAuth2 Client Credentials Flow.
	 *
	 * @return The refreshed credential
	 * @throws UnsupportedOperationException If the token endpoint type is not "QUERY" or "BODY"
	 * @throws RuntimeException If the response is unsuccessful
	 */
	public OAuth2Credential getCredentialByClientCredentials() {
		// request access token
		OkHttpClient client = new OkHttpClient();
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			Request request;
			
			if (tokenEndpointPostType.equalsIgnoreCase("QUERY")) {
				HttpUrl.Builder urlBuilder = HttpUrl.parse(this.tokenUrl).newBuilder();
				urlBuilder.addQueryParameter("client_id", this.clientId);
				urlBuilder.addQueryParameter("client_secret", this.clientSecret);
				urlBuilder.addQueryParameter("grant_type", "client_credentials");
				
				request = new Request.Builder()
						.url(urlBuilder.build().toString())
						.post(RequestBody.create(null, new byte[]{}))
						.build();
			} else if (tokenEndpointPostType.equalsIgnoreCase("BODY")) {
				RequestBody requestBody = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("client_id", this.clientId)
						.addFormDataPart("client_secret", this.clientSecret)
						.addFormDataPart("grant_type", "client_credentials")
						.build();
				
				request = new Request.Builder()
						.url(this.tokenUrl)
						.post(requestBody)
						.build();
			} else {
				throw new UnsupportedOperationException("Unknown tokenEndpointPostType: " + tokenEndpointPostType);
			}
			
			Response response = client.newCall(request).execute();
			String responseBody = response.body().string();
			if (response.isSuccessful()) {
				Map<String, Object> resultMap = objectMapper.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {});
				
				OAuth2Credential credential = new OAuth2Credential(this.providerName, (String) resultMap.get("access_token"), (String) resultMap.get("refresh_token"), null, null, (Integer) resultMap.get("expires_in"), null);
				return credential;
			} else {
				throw new RuntimeException("getCredentialByCode request failed! " + response.code() + ": " + responseBody);
			}
			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
