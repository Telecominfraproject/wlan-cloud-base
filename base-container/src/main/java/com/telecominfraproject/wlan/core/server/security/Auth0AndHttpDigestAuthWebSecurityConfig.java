package com.telecominfraproject.wlan.core.server.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * @author dtoptygin
 *
 */
@Configuration
@Profile(value = { "auth0_and_digest_auth" })
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class Auth0AndHttpDigestAuthWebSecurityConfig extends WebSecurityConfig {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        configureAuth0AndHttpDigestAuth(http);
    }

}
