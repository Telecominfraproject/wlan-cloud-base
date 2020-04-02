package com.telecominfraproject.wlan.core.server.security.webtoken;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Implements the org.springframework.security.core.Authentication interface.
 *
 */
public class WebtokenJWTToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 2168016239498023252L;

    private final String jwt;
    private final String fullUrl;
    private final String requestMethod;
	  private WebtokenUserDetails principal;

	public WebtokenJWTToken(String jwt, String fullUrl, String requestMethod) {
		super(null);
		this.jwt = jwt;
		this.fullUrl = fullUrl;
		this.requestMethod = requestMethod;
		setAuthenticated(false);
	}

	public String getJwt() {
		return jwt;
	}

	public String getFullUrl() {
        return fullUrl;
  }

  public String getRequestMethod() {
        return requestMethod;
  }

	public Object getCredentials() {
		return null;
	}

	public Object getPrincipal() {
		return principal;
	}

	public void setPrincipal(WebtokenUserDetails principal) {
		this.principal = principal;
	}

	@SuppressWarnings("unchecked")
	@Override
  public Collection<GrantedAuthority> getAuthorities() {
		return (Collection<GrantedAuthority>) principal.getAuthorities();
  }

}
