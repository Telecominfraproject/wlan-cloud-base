package com.telecominfraproject.wlan.core.server.security.webtoken;

import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;


/**
 * By default httpErrorCode is set to 401 (UNAUTHORIZED), but can be set to any other value by the caller.
 *  
 * @author dtop
 *
 */
public class WebtokenAuthTokenException extends AuthenticationException {

    private static final long serialVersionUID = 3366619189125874018L;
    private int httpErrorCode = HttpServletResponse.SC_UNAUTHORIZED;
    
    /**
     * httpErrorCode is set to 401 (UNAUTHORIZED)
     * @param msg
     */
    public WebtokenAuthTokenException(String msg) {
		super(msg);
	}

    public WebtokenAuthTokenException(String msg, int httpErrorCode) {
        super(msg);
        this.httpErrorCode = httpErrorCode;
    }

	public WebtokenAuthTokenException(String msg, Throwable t) {
		super(msg, t);
	}

	public WebtokenAuthTokenException(Exception e) {
		super(e.getMessage(), e);
	}

    public int getHttpErrorCode() {
        return httpErrorCode;
    }

    public void setHttpErrorCode(int httpErrorCode) {
        this.httpErrorCode = httpErrorCode;
    }

}
