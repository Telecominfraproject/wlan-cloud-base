package com.whizcontrol.core.server.container;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * @author dtop
 * 
 * This class wraps HttpServletRequest so that calls to getSession always return null.
 * It is used to disable session management on the server side (because it is not needed for REST APIs).
 */
public class NoSessionRequestWrapper extends HttpServletRequestWrapper{

    public NoSessionRequestWrapper(HttpServletRequest request) {
        super(request);
    }
    
    @Override
    public HttpSession getSession() {
        return null;
    }
    
    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

}