package com.telecominfraproject.wlan.core.server.security.webtoken;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.telecominfraproject.wlan.core.model.webtoken.IntrospectWebTokenResult;
import com.telecominfraproject.wlan.core.server.security.AccessType;
import com.telecominfraproject.wlan.core.server.security.AuthProviderInfo;
import com.telecominfraproject.wlan.core.server.security.Authority;

/**
 * Implementation of UserDetails in compliance with the decoded object returned
 * by the Extreme JWT
 * 
 */
public class WebtokenUserDetails implements UserDetails, AuthProviderInfo {

    private static final long serialVersionUID = 2099346099850627625L;
    
    private static final Logger LOG = LoggerFactory.getLogger(WebtokenUserDetails.class);

    private String username;
    private int customerId;
    private final IntrospectWebTokenResult introspectTokenResult;
    private Collection<GrantedAuthority> authorities = null;

    public WebtokenUserDetails(IntrospectWebTokenResult introspectTokenResult) {
        this.introspectTokenResult = introspectTokenResult;
        this.username = "INTEGRATION_USER";
        
        try{
            this.customerId = Integer.parseInt(introspectTokenResult.getCustomerId());
        }catch(Exception e){
            //this is legitimate case - not all tokens will resolve into a customerId
            LOG.trace("Cannot parse customerId from the introspection token result ", e);
        }

        // set authorities
        authorities = new ArrayList<>();
        authorities.add(Authority.TechSupport);
        
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
     * Will return the details of the attribute of JWT decoded token if it
     * exists or null otherwise. Example getUserAttribute("customerId")
     * 
     * @return return the details of the JWT decoded token if it exists or null
     *         otherwise
     */
    public Object getUserAttribute(String attributeName) {
        if("customerId".equals(attributeName)){
            return customerId;
        }
        
        return null;
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.Portal;
    }

    @Override
    public boolean isEnabled() {
        return introspectTokenResult.isActive();
    }
}
