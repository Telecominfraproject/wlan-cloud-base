package com.telecominfraproject.wlan.core.server.security.webtoken;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * @author dtop
 * Case 1:
 * If TSG component receives “HTTP status code 200 and Response Body contains Introspect Response object/entity ”,  
 *          then look for “errorCode” parameter from the response Body and send the same http response code to the caller of TSG APIs.
 * Case 2:
 * If TSG component receives  “HTTP status code other than 200  and Response Body contains Default REST-API Structure”,  
 *          send always 401 http response code (HttpServletResponse.SC_UNAUTHORIZED) to the caller of TSG APIs irrespective of received HTTP status code.
 * 
 */
public class WebtokenAuthenticationEntryPoint implements AuthenticationEntryPoint  {

	@SuppressWarnings("deprecation")
	@Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        PrintWriter writer = response.getWriter();

        if (isPreflight(request)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else if (authException instanceof WebtokenAuthTokenException) {
            int errorCode = ((WebtokenAuthTokenException) authException).getHttpErrorCode();
            response.setStatus(errorCode, authException.getMessage());
            writer.println("HTTP Status " + errorCode + " - " + authException.getMessage());
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
            writer.println("HTTP Status " + HttpServletResponse.SC_UNAUTHORIZED + " - " + authException.getMessage());
        }
    }	

    /**
     * Checks if this is a X-domain pre-flight request.
     * @param request
     * @return
     */
    private boolean isPreflight(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod());
    }
    
}