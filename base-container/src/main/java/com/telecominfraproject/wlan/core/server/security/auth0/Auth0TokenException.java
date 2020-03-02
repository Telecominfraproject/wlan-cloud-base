package com.telecominfraproject.wlan.core.server.security.auth0;

import org.springframework.security.core.AuthenticationException;


/**
 * Implementation of UserDetails in compliance with the decoded object returned by the Auth0 JWT 
 * 
 * @author Daniel Teixeira
 *
 */
public class Auth0TokenException extends AuthenticationException {

	private static final long serialVersionUID = -4495713385368912388L;

	public Auth0TokenException(String msg) {
		super(msg);
	}

	public Auth0TokenException(String msg, Throwable t) {
		super(msg, t);
	}

	public Auth0TokenException(Exception e) {
		super(e.getMessage(), e);
	}

}
