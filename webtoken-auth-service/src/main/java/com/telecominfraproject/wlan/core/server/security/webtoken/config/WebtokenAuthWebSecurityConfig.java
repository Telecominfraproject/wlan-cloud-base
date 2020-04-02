package com.telecominfraproject.wlan.core.server.security.webtoken.config;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.client.RestOperations;

import com.telecominfraproject.wlan.core.client.RestHttpClientConfig;
import com.telecominfraproject.wlan.core.server.security.WebSecurityConfig;
import com.telecominfraproject.wlan.core.server.security.webtoken.WebtokenAuthenticationEntryPoint;
import com.telecominfraproject.wlan.core.server.security.webtoken.WebtokenAuthenticationFilter;
import com.telecominfraproject.wlan.core.server.security.webtoken.WebtokenAuthenticationProvider;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * @author dtoptygin
 *
 */
@Configuration
@Profile(value = { "use_webtoken_auth" })
@EnableWebSecurity
public class WebtokenAuthWebSecurityConfig extends WebSecurityConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebtokenAuthWebSecurityConfig.class);

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        configureWebtokenAuth(http);
    }
 
    
    @Bean
    @Profile(value = { "use_webtoken_auth" })
    public WebtokenAuthenticationEntryPoint webtokenAuthenticationEntryPoint(){
        WebtokenAuthenticationEntryPoint ret = new WebtokenAuthenticationEntryPoint();
        return ret;
    } 

    @Bean
    @Profile(value = { "use_webtoken_auth" })
    public WebtokenAuthenticationProvider webtokenAuthenticationProvider(RestOperations restTemplate, Environment env, RestHttpClientConfig restHttpClientConfig){
        String introspectTokenApiHost = env.getProperty("tip.wlan.introspectTokenApi.host", 
                "localhost:9070"
                );
        String introspectTokenApiClientToken = env.getProperty("tip.wlan.introspectTokenApi.clientToken", 
                "token_placeholder");
        
        restHttpClientConfig.skipPreemptiveAuthentication(new HttpHost(introspectTokenApiHost, -1, "https"));
        
        if(introspectTokenApiHost.contains(":")){
            String host = introspectTokenApiHost.split(":")[0];
            int port = Integer.parseInt(introspectTokenApiHost.split(":")[1]);
            restHttpClientConfig.skipPreemptiveAuthentication(new HttpHost(host, port, "https"));
        }
        
        WebtokenAuthenticationProvider ret = new WebtokenAuthenticationProvider(restTemplate, introspectTokenApiHost, introspectTokenApiClientToken);

        return ret;
    } 

    @Bean
    @Profile(value = { "use_webtoken_auth" })
    public WebtokenAuthenticationFilter webtokenAuthenticationFilter(WebtokenAuthenticationEntryPoint eaEntryPoint, WebtokenAuthenticationProvider eaProvider) {
        WebtokenAuthenticationFilter ret = new WebtokenAuthenticationFilter();
        
        ret.setEntryPoint(eaEntryPoint);
        
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
        authenticationProviders.add(eaProvider);
        AuthenticationManager authenticationManager = new ProviderManager(authenticationProviders );        
        
        ret.setAuthenticationManager(authenticationManager);
        
        return ret;
    }

    
    protected void configureWebtokenAuth(HttpSecurity http) {
        LOG.info("configuring Webtoken authentication");

        try {

            http.exceptionHandling()
                    // these entry points handle cases when request is made to a
                    // protected page and
                    // user cannot be authenticated
                    .defaultAuthenticationEntryPointFor(
                            applicationContext.getBean(WebtokenAuthenticationEntryPoint.class), new RequestMatcher() {
                                @Override
                                public boolean matches(HttpServletRequest request) {
                                    //APIs provide Authorization header with the Bearer token
                                    return WebtokenAuthenticationFilter.getToken(request) != null;
                                }
                            });

            configureProtectedPaths(http);
            
            http.addFilterBefore(applicationContext.getBean(WebtokenAuthenticationFilter.class), DigestAuthenticationFilter.class);

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        commonConfiguration(http);
    } 
}
