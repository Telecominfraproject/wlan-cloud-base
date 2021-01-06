package com.telecominfraproject.wlan.core.server.security.auth0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.telecominfraproject.wlan.core.server.security.AccessType;
import com.telecominfraproject.wlan.core.server.security.AuthProviderInfo;
import com.telecominfraproject.wlan.core.server.security.Authority;

/**
 * Implementation of UserDetails in compliance with the decoded object returned
 * by the Auth0 JWT
 * 
 * @author Daniel Teixeira
 *
 */
public class Auth0UserDetails implements UserDetails, AuthProviderInfo {

    private static final long serialVersionUID = 2058797193125711681L;

    private DecodedJWT details;
    private String username;
    private boolean emailVerified = false;
    private Collection<GrantedAuthority> authorities = null;
    private final AccessType accessType;
    private static final String EMAIL_CLAIM = "email";
    private static final String EMAIL_VERIFIED_CLAIM = "email_verified";
    private static final String NICKNAME_CLAIM = "nickname";
    private static final String ROLES_CLAIM = "roles";

    private static final Log LOGGER = LogFactory.getLog(Auth0UserDetails.class);
    
    public Auth0UserDetails(DecodedJWT jwt, AccessType accessType) {
    	this(jwt, accessType, null);
    }

	public Auth0UserDetails(DecodedJWT jwt, AccessType accessType, String claimsUrl) {
        this.accessType = accessType;
        String emailClaim;
        String emailVerifiedClaim;
        String nicknameClaim;
        String rolesClaim;
        if (claimsUrl != null) {
        	emailClaim = claimsUrl + ":" + EMAIL_CLAIM;
        	emailVerifiedClaim = claimsUrl + ":" + EMAIL_VERIFIED_CLAIM;
        	nicknameClaim = claimsUrl + ":" + NICKNAME_CLAIM;
        	rolesClaim = claimsUrl + ":" + ROLES_CLAIM;
        } else {
        	emailClaim = EMAIL_CLAIM;
        	emailVerifiedClaim = EMAIL_VERIFIED_CLAIM;
        	nicknameClaim = NICKNAME_CLAIM;
        	rolesClaim = ROLES_CLAIM;
        }
        if (!jwt.getClaim(emailClaim).isNull()) {
            this.username = jwt.getClaim(emailClaim).asString();
        } else if (!jwt.getClaim(nicknameClaim).isNull()) {
            this.username = jwt.getClaim(nicknameClaim).asString();
        } else if (jwt.getId() != null) {
            this.username = jwt.getId();
        } else if (jwt.getSubject() != null) {
            this.username = jwt.getSubject();
        } else {
            this.username = "UNKNOWN_USER";
        }

        if (!jwt.getClaim(emailClaim).isNull()) {
            this.emailVerified = Boolean.valueOf(jwt.getClaim(emailVerifiedClaim).toString());
        }

        // set authorities
        authorities = new ArrayList<>();
    	if (!jwt.getClaim(rolesClaim).isNull()) {
            List<String> roles = null;
            try {
				roles = jwt.getClaim(rolesClaim).asList(String.class);
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority(role));
                }
            } catch (java.lang.ClassCastException e) {
                LOGGER.error("Error in casting the roles object", e);
            }
        }

        // By default, set to CustomerIT authority if nothing is added
        if (authorities.isEmpty()) {
            authorities.add(Authority.CustomerIT);
        }

        this.details = jwt;

    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Will return UnsupportedOperationException
     */
    public String getPassword() {
        throw new UnsupportedOperationException("Password is protected");
    }

    /**
     * Gets the email if it exists otherwise it returns the user_id
     */
    public String getUsername() {
        return username;
    }

    /**
     * Will return false
     */
    public boolean isAccountNonExpired() {
        return false;
    }

    /**
     * Will return false
     */
    public boolean isAccountNonLocked() {
        return false;
    }

    /**
     * Will return false
     */
    public boolean isCredentialsNonExpired() {
        return false;
    }

    /**
     * Will return true if the email is verified, otherwise it will return false
     */
    public boolean isEnabled() {
        return emailVerified;
    }

    /**
     * Will return the details of the attribute of JWT decoded token if it
     * exists or null otherwise. Example getAuth0Attribute("email"),
     * getAuth0Attribute("picture")....
     * 
     * @return return the details of the JWT decoded token if it exists or null
     *         otherwise
     */
    public Object getAuth0Attribute(String attributeName) {
        if (details.getClaim(attributeName).isNull()) {
        	LOGGER.debug("No attribute was found : " + attributeName);
        	return null;
        }
        return details.getClaim(attributeName);
    }

    @Override
    public AccessType getAccessType() {
        return this.accessType;
    }

}
