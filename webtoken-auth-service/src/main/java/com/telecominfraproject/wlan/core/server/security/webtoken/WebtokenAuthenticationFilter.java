package com.telecominfraproject.wlan.core.server.security.webtoken;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

import com.telecominfraproject.wlan.core.server.security.cors.StaticCorsFilter;

/**
 * Filter responsible to intercept the JWT in the HTTP header and attempt an
 * authentication. It delegates the authentication to the authentication manager
 *
 */
public class WebtokenAuthenticationFilter extends GenericFilterBean {

    private AuthenticationManager authenticationManager;

    private AuthenticationEntryPoint entryPoint;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        if (!response.containsHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
            StaticCorsFilter.setCorsHeaders(response);
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            // Auth0 has unsuccessfully tried to authenticate request, now let's
            // try Extreme token introspection API

            if ("OPTIONS".equals(request.getMethod())) {
                // This is CORS request
                chain.doFilter(request, response);
                return;
            }

            if (request.getRequestURI().startsWith("/management/v1/oauth2/")) {
                // This is token request, should be unprotected, ignore it
                chain.doFilter(request, response);
                return;
            }

            String jwt = getToken((HttpServletRequest) request);

            if (jwt != null) {
                try {

                    StringBuffer fullUrl = request.getRequestURL();
                    if (request.getQueryString() != null) {
                        fullUrl.append("?").append(request.getQueryString());
                    }

                    WebtokenJWTToken token = new WebtokenJWTToken(jwt, fullUrl.toString(), request.getMethod());

                    Authentication authResult = getAuthenticationManager().authenticate(token);
                    SecurityContextHolder.getContext().setAuthentication(authResult);

                } catch (AuthenticationException failed) {
                    SecurityContextHolder.clearContext();
                    entryPoint.commence(request, response, failed);
                    return;
                }
            }

        } else {
            // Auth0 has successfully authenticated request, no need to do
            // anything else
        }

        chain.doFilter(request, response);

    }

    /**
     * Looks at the authorization bearer and extracts the JWT
     */
    public static String getToken(HttpServletRequest httpRequest) {
        String token = null;
        final String authorizationHeader = httpRequest.getHeader("authorization");
        if (authorizationHeader == null) {
            // "Unauthorized: No Authorization header was found"
            return null;
        }

        int idx = authorizationHeader.indexOf(' ');

        if (idx < 0) {
            // "Unauthorized: Format is Authorization: Bearer [token]
            // optional_extra_str"
            return null;

        }

        String scheme = authorizationHeader.substring(0, idx);
        String credentials = authorizationHeader.substring(idx + 1);

        if ("Bearer".equalsIgnoreCase(scheme)) {
            token = credentials;
        }
        return token;
    }

    public AuthenticationEntryPoint getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(AuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

}