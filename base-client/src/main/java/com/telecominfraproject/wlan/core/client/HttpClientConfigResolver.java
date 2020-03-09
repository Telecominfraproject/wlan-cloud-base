package com.telecominfraproject.wlan.core.client;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import com.telecominfraproject.wlan.core.client.models.HttpClientConfig;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * @author dtoptygin
 * 
 */
@Configuration
@Profile(value = {"rest-template-single-user-per-service-digest-auth", "RestTemplateConfiguration_X509_client_cert_auth", "multi-identity-client-cert-auth"})
public class HttpClientConfigResolver {

    private static final Logger LOG = LoggerFactory
            .getLogger(HttpClientConfigResolver.class);

    private static final String DEFAULT_CONFIG_LOCATION = "classpath:httpClientConfig.json";

    @Bean
    HttpClientConfig configureHttpClientConfig(Environment environment) {
        try {
            String configLocation;
            if(environment!=null){
                configLocation = environment.getProperty("whizcontrol.httpClientConfig",DEFAULT_CONFIG_LOCATION);
            }
            else {
                configLocation = System.getProperty("whizcontrol.httpClientConfig",DEFAULT_CONFIG_LOCATION);
            }
            LOG.info("Loading http client configuration from {}",
                    configLocation);
            Object configContent = ResourceUtils.getURL(configLocation)
                    .getContent();
            HttpClientConfig ret = null;
            if (configContent instanceof String) {
                ret = HttpClientConfig.fromString((String) configContent,
                        HttpClientConfig.class);
            } else if (configContent instanceof InputStream) {
                ret = HttpClientConfig.fromString(StreamUtils.copyToString((InputStream) configContent, StandardCharsets.UTF_8),
                        HttpClientConfig.class);
            }
            LOG.info("Got http client configuration from {}", configLocation);
            return ret;
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot read http client configuration : ", e);
        }
    }

}
