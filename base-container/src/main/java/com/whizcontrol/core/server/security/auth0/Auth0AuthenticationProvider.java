package com.whizcontrol.core.server.security.auth0;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.auth0.jwt.JWTVerifier;
import com.whizcontrol.core.server.security.AccessType;
import com.whizcontrol.server.exceptions.ConfigurationException;

/**
 * Class that verifies the JWT token and in case of beeing valid, it will set
 * the userdetails in the authentication object
 * 
 * @author Daniel Teixeira
 */
public class Auth0AuthenticationProvider implements AuthenticationProvider, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(Auth0AuthenticationProvider.class);

    private JWTVerifier jwtVerifier = null;
    private String clientSecret = null;
    private String clientId = null;
    private String securedRoute = null;
    private final AccessType accessType;
    private static final AuthenticationException AUTH_ERROR = new Auth0TokenException("Authentication error occured");

    public Auth0AuthenticationProvider(AccessType accessType) {
        this.accessType = accessType;
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String token = ((Auth0JWTToken) authentication).getJwt();

        LOG.trace("Auth0 trying to authenticate with token: {} ", token);

        Map<String, Object> decoded;
        try {

            Auth0JWTToken tokenAuth = ((Auth0JWTToken) authentication);
            decoded = jwtVerifier.verify(token);
            LOG.trace("Decoded JWT token {}", decoded);
            tokenAuth.setAuthenticated(true);
            tokenAuth.setPrincipal(new Auth0UserDetails(decoded, this.accessType));
            tokenAuth.setDetails(decoded);
            return authentication;

        } catch (InvalidKeyException e) {
            LOG.error("InvalidKeyException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        } catch (NoSuchAlgorithmException e) {
            LOG.error("NoSuchAlgorithmException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        } catch (IllegalStateException e) {
            LOG.error("IllegalStateException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        } catch (SignatureException e) {
            LOG.debug("SignatureException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        } catch (IOException e) {
            LOG.error("IOException thrown while decoding JWT token", e);
            throw AUTH_ERROR;
        }
    }

    public boolean supports(Class<?> authentication) {
        return Auth0JWTToken.class.isAssignableFrom(authentication);
    }

    public void afterPropertiesSet() throws Exception {
        if ((clientSecret == null) || (clientId == null)) {
            throw new ConfigurationException("Client secret or client id is not set for Auth0AuthenticationProvider");
        }
        if (securedRoute == null) {
            throw new ConfigurationException("SecureRoute is not set for Auth0AuthenticationProvider");
        }
        jwtVerifier = new JWTVerifier(clientSecret, clientId);
    }

    public String getSecuredRoute() {
        return securedRoute;
    }

    public void setSecuredRoute(String securedRoute) {
        this.securedRoute = securedRoute;
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
