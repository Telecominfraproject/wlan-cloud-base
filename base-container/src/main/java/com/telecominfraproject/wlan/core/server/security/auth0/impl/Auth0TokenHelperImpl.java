package com.telecominfraproject.wlan.core.server.security.auth0.impl;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecominfraproject.wlan.core.server.security.auth0.Auth0TokenHelper;

public class Auth0TokenHelperImpl implements Auth0TokenHelper<Object>, InitializingBean {
	
	private static final Log Logger = LogFactory.getLog(Auth0TokenHelperImpl.class);
	
	private String clientSecret = null;
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
		
		JwkProvider jwkProvider = new UrlJwkProvider(issuer);
				
		try {
			DecodedJWT jwt = JWT.decode(token);
            String alg = jwt.getAlgorithm();
            
            // Get jwk
            Jwk jwk = jwkProvider.get(jwt.getKeyId());
            
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
			@SuppressWarnings("unchecked")
			Map<String, String> map = new ObjectMapper().readValue(jwt.getPayload(), Map.class);
			return map;

		} catch (IllegalStateException|IOException|JwkException e) {
			throw new Auth0RuntimeException(e);
		}
		
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(clientSecret, "The client secret is not set for " + this.getClass());
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

}
