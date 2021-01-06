package com.telecominfraproject.wlan.core.server.security.auth0;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.ResourceUtils;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.telecominfraproject.wlan.core.server.security.AccessType;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * Class that verifies the JWT token and in case of being valid, it will set
 * the userdetails in the authentication object
 * 
 * @author Daniel Teixeira
 * @author rlee
 */
public class Auth0AuthenticationProvider implements AuthenticationProvider, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(Auth0AuthenticationProvider.class);
    
    private ObjectMapper mapper = new ObjectMapper();

    private String clientSecret = null;
    private String issuer = null;
    private String jwksLocation = null;
    private String claimsUrl = null;
    private final AccessType accessType;
    private static final AuthenticationException AUTH_ERROR = new Auth0TokenException("Authentication error occured");
    
    public Auth0AuthenticationProvider(AccessType accessType) {
        this.accessType = accessType;
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String token = ((Auth0JWTToken) authentication).getJwt();
        LOG.trace("Auth0 trying to authenticate with token: {} ", token);
                        
        try {
            Auth0JWTToken tokenAuth = ((Auth0JWTToken) authentication);
            
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
            LOG.trace("Decoded JWT token {}", jwt);
            tokenAuth.setAuthenticated(true);
            tokenAuth.setPrincipal(new Auth0UserDetails(jwt, this.accessType, claimsUrl));
            tokenAuth.setDetails(jwt);
            return authentication;

        } catch (JWTDecodeException e) {
            LOG.error("JWTDecodeException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        } catch (JWTVerificationException e) {
            LOG.error("JWTVerificationException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        } catch (JwkException e) {
            LOG.error("JwkException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        } catch (IllegalStateException e) {
            LOG.error("IllegalStateException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        }
    }

    public boolean supports(Class<?> authentication) {
        return Auth0JWTToken.class.isAssignableFrom(authentication);
    }

    public void afterPropertiesSet() throws Exception {
        if ((clientSecret == null) || (issuer == null)) {
            throw new ConfigurationException("Client secret, client id, or issuer URI is not set for Auth0AuthenticationProvider");
        }
    }
    
    private Jwk getJwk(String keyId) {
        try {      	
        	String jwksSource = getJwksString();
        	if (jwksSource == null) {
            	throw new FileNotFoundException("jwks could not be found");
            }
        	
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
                    	// Can only contain 1 matching jwk
                        return jwk;
                    }
                }
            }
            
        } catch (JsonMappingException e) {
        	LOG.error("JsonMappingException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
		} catch (JsonProcessingException e) {
			LOG.error("JsonProcessingException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
		} catch (FileNotFoundException e) {
			LOG.error("FileNotFoundException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
		}
        
        return null;
    }
    
    private String getJwksString() {
    	LOG.debug("Loading jwks from {}", jwksLocation);		
    	String ret = null;
    	
    	try {
	    	Object jwksObj = ResourceUtils.getURL(jwksLocation).getContent();
	    	if (jwksObj instanceof InputStream) {
	    		ret = readFromInputStream((InputStream) jwksObj);
	    	}
    	} catch (FileNotFoundException e) {
    		LOG.error("FileNotFoundException thrown while getting jwks", e);
            throw AUTH_ERROR;
    	} catch (IOException e) {
    		LOG.error("IOException thrown while getting jwks", e);
            throw AUTH_ERROR;
		}
	  return ret;
	}
    
    private String readFromInputStream(InputStream inputStream) {
    	StringBuilder resultStringBuilder = new StringBuilder();
	    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
	        String line;
	        while ((line = br.readLine()) != null) {
	            resultStringBuilder.append(line).append("\n");
	        }
	    } catch (IOException e) {
	    	LOG.error("IOException thrown while getting jwks", e);
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

    public String getIssuer() {
    	return issuer;
    }
    
    public void setIssuer(String issuer) {
    	this.issuer = issuer;
    }
    
    public String getJwksLocation() {
    	return jwksLocation;
    }
    
    public void setJwksLocation(String jwksLocation) {
    	this.jwksLocation = jwksLocation;
    }

    public String getClaimsUrl() {
		return claimsUrl;
	}

	public void setClaimsUrl(String claimsUrl) {
		this.claimsUrl = claimsUrl;
	}

	/**
     * Use to encode raw secret to Base 64 URL safe string
     * 
     * @param args
     *            list of raw value
     */
    public static void main(String args[]) {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter raw client secret [q to quit]: ");
        while (input.hasNext()) {
            String raw = input.nextLine();
            if ("q".equals(raw)) {
                break;
            }
            System.out.println(String.format("B64Urlsafe: %s",
                    org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(raw.getBytes())));
            System.out.print("Enter raw client secret [Enter to exit]: ");
        }
        System.out.println("Done");
        input.close();
    }
}
