package com.telecominfraproject.wlan.core.server.security.auth0.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.telecominfraproject.wlan.core.server.security.auth0.Auth0TokenException;
import com.telecominfraproject.wlan.core.server.security.auth0.Auth0TokenHelper;

public class Auth0TokenHelperImpl implements Auth0TokenHelper<Object>, InitializingBean {
	
	private static final Log Logger = LogFactory.getLog(Auth0TokenHelperImpl.class);
	
	private ObjectMapper mapper = new ObjectMapper();
    private static final AuthenticationException AUTH_ERROR = new Auth0TokenException("Authentication error occured");
	
	private String clientSecret = null;
	private String clientId = null;
	private String issuer = null;

	@Override
	public String generateToken(Object object, int expiration) {

		String token;
		try {
			
			Algorithm hsEncoded = Algorithm.HMAC256(clientSecret);
			token = JWT.create()
					.withIssuer(issuer)
					.withExpiresAt(new Date(expiration))
					.withClaim("payload", new ObjectMapper().writeValueAsString(object))
					.sign(hsEncoded);
		
		} catch (JsonProcessingException e) {
			throw new Auth0RuntimeException(e);
		}
		
		return token;
		
	}

	@Override
	public Object decodeToken(String token) {
				
		try {
			DecodedJWT jwt = JWT.decode(token);
            String alg = jwt.getAlgorithm();
            
            // Get jwks file
            Jwk jwk = getJwk(jwt.getKeyId());
            if (jwk == null) {
            	throw new JwkException("jwk could not be found");
            }
            
            Algorithm algorithm;
            if (alg.equals("RS256")) {
            	// create RS256 key decoder
            	algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            } else {
            	// create HS256 key decoder
            	algorithm = Algorithm.HMAC256(clientSecret);
            }
            
            JWTVerifier verifier = JWT.require(algorithm)
            		.withIssuer(issuer)
            		.build();
            
            jwt = verifier.verify(token);
			String payload = jwt.getPayload();
			@SuppressWarnings("unchecked")
			Map<String, String> map = new ObjectMapper().readValue(payload, Map.class);
			return map;

		} catch (IllegalStateException|IOException|JwkException e) {
			throw new Auth0RuntimeException(e);
		}
		
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(clientSecret, "The client secret is not set for " + this.getClass());
		Assert.notNull(clientId, "The client id is not set for " + this.getClass());
	}
	
	private Jwk getJwk(String keyId) {
        try {
        	InputStream is = getClass().getClassLoader().getResourceAsStream("jwks.json");
        	String jwksSource = readFromInputStream(is);
        	
        	List<Jwk> jwks = Lists.newArrayList();
        	@SuppressWarnings("unchecked")
    		List<Map<String, Object>> keys = (List<Map<String, Object>>) mapper.readValue(jwksSource, Map.class).get("keys");
            
            for (Map<String, Object> values : keys) {
                jwks.add(Jwk.fromValues(values));
            }
            if (keyId == null && jwks.size() == 1) {
                return jwks.get(0);
            }
            if (keyId != null) {
                for (Jwk jwk : jwks) {
                    if (keyId.equals(jwk.getId())) {
                        return jwk;
                    }
                }
            }
        } catch (JsonMappingException e) {
        	Logger.error("JsonMappingException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
		} catch (JsonProcessingException e) {
			Logger.error("JsonProcessingException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
		}
        
        return null;
    }
    
    private String readFromInputStream(InputStream inputStream) {
	    StringBuilder resultStringBuilder = new StringBuilder();
	    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
	        String line;
	        while ((line = br.readLine()) != null) {
	            resultStringBuilder.append(line).append("\n");
	        }
	    } catch (IOException e) {
	    	Logger.error("IOException thrown while getting jwks", e);
            throw AUTH_ERROR;
	    }
	  return resultStringBuilder.toString();
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

}
