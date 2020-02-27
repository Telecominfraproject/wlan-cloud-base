package com.whizcontrol.core.server.security;

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
@Profile(value = { "client_certificate_and_basic_auth" })
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class X509CertificateAndHttpBasicAuthWebSecurityConfig extends WebSecurityConfig {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        configureX509CertificateAndHttpBasicAuth(http);
    }

}
