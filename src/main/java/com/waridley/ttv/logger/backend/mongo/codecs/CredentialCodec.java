package com.waridley.ttv.logger.backend.mongo.codecs;

import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.waridley.ttv.logger.backend.NamedOAuth2Credential;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.*;

public class CredentialCodec implements Codec<Credential> {
	
	private CodecRegistry codecRegistry;
	
	public CredentialCodec(CodecRegistry codecRegistry) {
		this.codecRegistry = codecRegistry;
	}
	
	@Override
	public Credential decode(BsonReader reader, DecoderContext decoderContext) {
		Credential credential = null;
		String userId = null;
		String identityProvider = null;
		Map<String, Object> additionalValues = new HashMap<>();
		
		Optional<String> name = Optional.empty();
		Optional<OAuth2Credential> oAuth2Credential = Optional.empty();
		
		Optional<String> accessToken = Optional.empty();
		Optional<String> refreshToken = Optional.empty();
		Optional<String> userName = Optional.empty();
		Optional<Integer> expiresIn = Optional.empty();
		List<String> scopes = new ArrayList<>();
		
		reader.readStartDocument();
		while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
			
			String fieldName = reader.readName();
			switch(fieldName) {
				case("userId"):
					userId = reader.readString();
					break;
				case("identityProvider"):
					identityProvider = reader.readString();
					break;
				case("name"):
					name = Optional.of(reader.readString());
					break;
				case("credential"):
					oAuth2Credential = Optional.of(new OAuth2Codec().decode(reader, decoderContext));
					break;
				case("accessToken"):
					accessToken = Optional.of(reader.readString());
					break;
				case("refreshToken"):
					refreshToken = Optional.of(reader.readString());
					break;
				case("userName"):
					userName = Optional.of(reader.readString());
					break;
				case("expiresIn"):
					expiresIn = Optional.of(reader.readInt32());
					break;
				case("scopes"):
					reader.readStartArray();
					while(reader.readBsonType() == BsonType.STRING) scopes.add(reader.readString());
					reader.readEndArray();
					break;
				default:
					reader.skipValue();
					//readAdditionalValues(reader, fieldName, decoderContext, additionalValues);
			}
			
		}
		reader.readEndDocument();
		
		if(name.isPresent()) {
			credential = new NamedOAuth2Credential(name.get(), oAuth2Credential.orElse(null));
		} else if(accessToken.isPresent()) {
			credential = new OAuth2Credential(
					identityProvider,
					accessToken.get(),
					refreshToken.orElse(null),
					userId,
					userName.orElse(null),
					expiresIn.orElse(null),
					scopes);
		} else if(additionalValues.size() > 0) {
			credential = new UnknownCredential(identityProvider, userId, additionalValues);
		} else {
			credential = new Credential(userId, identityProvider) { };
		}
		
		return credential;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void encode(BsonWriter writer, Credential credential, EncoderContext encoderContext) {
		if(credential.getClass() != Credential.class) {
			try {
				//Codec codec = codecRegistry.get(credential.getClass());
				//if(!codec.getEncoderClass().equals(getEncoderClass())) codec.encode(writer, credential, encoderContext);
				//else encodeCredentialInterface(writer, credential, encoderContext);
				encodeCredentialInterface(writer, credential, encoderContext);
			} catch(CodecConfigurationException e) {
				encodeCredentialInterface(writer, credential, encoderContext);
			}
		} else {
			encodeCredentialInterface(writer, credential, encoderContext);
		}
		
	}
	
	private void encodeCredentialInterface(BsonWriter writer, Credential credential, EncoderContext encoderContext) {
		writer.writeStartDocument();
		if(credential.getUserId() != null) {
			writer.writeName("userId");
			writer.writeString(credential.getUserId());
		}
		if(credential.getIdentityProvider() != null) {
			writer.writeName("identityProvider");
			writer.writeString(credential.getIdentityProvider());
		}
		writer.writeEndDocument();
	}
	
	@Override
	public Class<Credential> getEncoderClass() {
		return Credential.class;
	}
	
	private void readAdditionalValues(BsonReader reader, String fieldName, DecoderContext decoderContext, Map<String, Object> additionalValues) {
		if(reader.getCurrentBsonType() == BsonType.OBJECT_ID) additionalValues.put(fieldName, reader.readObjectId());
		else if(reader.getCurrentBsonType() == BsonType.STRING) additionalValues.put(fieldName, reader.readString());
		else if(reader.getCurrentBsonType() == BsonType.INT32) additionalValues.put(fieldName, reader.readInt32());
		else if(reader.getCurrentBsonType() == BsonType.INT64) additionalValues.put(fieldName, reader.readInt64());
		else if(reader.getCurrentBsonType() == BsonType.DECIMAL128) additionalValues.put(fieldName, reader.readDecimal128());
		else if(reader.getCurrentBsonType() == BsonType.DOUBLE) additionalValues.put(fieldName, reader.readDouble());
		else if(reader.getCurrentBsonType() == BsonType.DOCUMENT) additionalValues.put(fieldName, decoderContext.decodeWithChildContext(codecRegistry.get(Document.class), reader));
		else if(reader.getCurrentBsonType() == BsonType.BOOLEAN) additionalValues.put(fieldName, reader.readBoolean());
		else if(reader.getCurrentBsonType() == BsonType.DATE_TIME) additionalValues.put(fieldName, reader.readDateTime());
		else if(reader.getCurrentBsonType() == BsonType.BINARY) additionalValues.put(fieldName, reader.readBinaryData());
		else if(reader.getCurrentBsonType() == BsonType.DB_POINTER) additionalValues.put(fieldName, reader.readDBPointer());
		else if(reader.getCurrentBsonType() == BsonType.JAVASCRIPT) additionalValues.put(fieldName, reader.readJavaScript());
		else if(reader.getCurrentBsonType() == BsonType.JAVASCRIPT_WITH_SCOPE) additionalValues.put(fieldName, reader.readJavaScriptWithScope());
		else if(reader.getCurrentBsonType() == BsonType.MAX_KEY) reader.readMaxKey();
		else if(reader.getCurrentBsonType() == BsonType.MIN_KEY) reader.readMinKey();
		else if(reader.getCurrentBsonType() == BsonType.REGULAR_EXPRESSION) additionalValues.put(fieldName, reader.readRegularExpression());
		else if(reader.getCurrentBsonType() == BsonType.TIMESTAMP) additionalValues.put(fieldName, reader.readTimestamp());
		else if(reader.getCurrentBsonType() == BsonType.SYMBOL) additionalValues.put(fieldName, reader.readSymbol());
		else if(reader.getCurrentBsonType() == BsonType.UNDEFINED) reader.readUndefined();
		else if(reader.getCurrentBsonType() == BsonType.NULL) {
			reader.readNull();
			additionalValues.put(fieldName, null);
		}
		else if(reader.getCurrentBsonType() == BsonType.ARRAY) {
			reader.readStartArray();
				BsonType type = reader.getCurrentBsonType();
				if(type == BsonType.STRING) while(reader.getCurrentBsonType() == BsonType.STRING) additionalValues.put(fieldName, reader.readString());
				//TODO etc...
			reader.readEndArray();
		}
		else { reader.skipValue(); }
	}
	
}

class UnknownCredential extends Credential {
	
	Map<String, Object> additionalValues;
	Map<String, Object> getAdditionalValues() { return additionalValues; }
	/**
	 * Credential
	 *
	 * @param identityProvider Identity Provider
	 * @param userId           User Id
	 */
	public UnknownCredential(String identityProvider, String userId, Map<String, Object> additionalValues) {
		super(identityProvider, userId);
		this.additionalValues = additionalValues;
	}
}