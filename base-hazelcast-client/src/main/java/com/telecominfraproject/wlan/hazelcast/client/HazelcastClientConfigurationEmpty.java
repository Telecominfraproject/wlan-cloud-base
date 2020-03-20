package com.telecominfraproject.wlan.hazelcast.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.hazelcast.core.HazelcastInstance;

/**
 * @author erik
 *
 */
@Configuration
@Profile("use-hazelcast-empty")
public class HazelcastClientConfigurationEmpty {

    @Bean
    HazelcastInstance hazelcastClient() {
        return null;
    }

}
