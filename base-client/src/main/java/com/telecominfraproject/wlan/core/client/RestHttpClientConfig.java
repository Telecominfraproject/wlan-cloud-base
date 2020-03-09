package com.telecominfraproject.wlan.core.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpHost;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestHttpClientConfig {
    private static final Boolean DEFAULT_SETTING = true;
    private Map<HttpHost, Boolean> preemptiveHttpSettings = new ConcurrentHashMap<>();
    
    public void skipPreemptiveAuthentication(HttpHost host) {
        preemptiveHttpSettings.put(host, false);
    }

    public boolean isPreemptive(HttpHost targetHost) {
        Boolean result = preemptiveHttpSettings.get(targetHost);
        return (result == null) ? DEFAULT_SETTING : result;
    }
}
