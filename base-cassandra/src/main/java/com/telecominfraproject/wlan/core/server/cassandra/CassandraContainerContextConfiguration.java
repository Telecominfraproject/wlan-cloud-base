package com.telecominfraproject.wlan.core.server.cassandra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;

@Component
public class CassandraContainerContextConfiguration {
	
	@Autowired
	private CqlSession cqlSession;

	private static final Logger LOG = LoggerFactory.getLogger(CassandraContainerContextConfiguration.class);

    @Bean
    public ApplicationListener<ContextClosedEvent> cassandraContainerStopEventListner() {
        LOG.debug("Creating cassandra container stop event listener");
        return new CassandraConsumerStopListener(cqlSession);
    }
    
}
