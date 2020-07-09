package com.telecominfraproject.wlan.core.server.cassandra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;

/**
 * Register for container stop event so that we can close the Cassandra CQL session
 * 
 * @author dtop
 *
 */
@Component
public class CassandraConsumerStopListener implements ApplicationListener<ContextClosedEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(CassandraConsumerStopListener.class);

	@Autowired
	private CqlSession cqlSession;

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		LOG.debug("Processing ContextClosedEvent event");
    	if(cqlSession!=null) {
    		cqlSession.close();
    	}
	}

}
