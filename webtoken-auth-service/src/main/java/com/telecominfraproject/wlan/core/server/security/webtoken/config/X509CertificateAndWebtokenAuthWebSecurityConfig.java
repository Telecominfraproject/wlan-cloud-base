package com.telecominfraproject.wlan.core.server.security.webtoken.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * @author dtoptygin
 *
 */
@Configuration
@Profile(value = { "client_certificate_and_webtoken_auth" })
@EnableWebSecurity
public class X509CertificateAndWebtokenAuthWebSecurityConfig extends WebtokenAuthWebSecurityConfig {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        configureX509CertificateAndWebtokenAuth(http);
    }

}
