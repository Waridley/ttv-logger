package com.waridley.ttv.logger.backend.mongo.codecs;

import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.waridley.ttv.logger.backend.NamedOAuth2Credential;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.List;

public class CredentialCodecProvider implements CodecProvider {
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
		if(OAuth2Credential.class.isAssignableFrom(clazz)) {
			return (Codec<T>) new OAuth2Codec();
		} else if(NamedOAuth2Credential.class.isAssignableFrom(clazz)) {
			List<Convention> conventions = new ArrayList<>(Conventions.DEFAULT_CONVENTIONS);
			conventions.add(Conventions.SET_PRIVATE_FIELDS_CONVENTION);
			PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder()
					.automatic(false)
					.conventions(conventions)
					.register(NamedOAuth2Credential.class)
					.build();
			return pojoCodecProvider.get(clazz, registry);
		} else if(Credential.class.isAssignableFrom(clazz)) {
			return (Codec<T>) new CredentialCodec(registry);
		}
		
		return null;
	}
}
