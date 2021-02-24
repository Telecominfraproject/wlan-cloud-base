package com.telecominfraproject.wlan.core.server.cassandra;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;

/**
 * @author dtop
 *
 */
@Configuration
public class BaseCassandraDataSource {
	
    private static final Logger LOG = LoggerFactory.getLogger(BaseCassandraDataSource.class);

	@Value("${tip.wlan.cassandraConfigFileName:/app/config/cassandra-application.conf}")
	private String cassandraConfigFileName;

	@Bean
	public CqlSession cqlSession() {
		/*
		 * See: https://docs.datastax.com/en/developer/java-driver/4.7/manual/core/
		 * 
		 * CqlSession is the main entry point of the driver. It holds the known state of
		 * the actual Cassandra cluster, and is what you use to execute queries. It is
		 * thread-safe, you should create a single instance (per target Cassandra
		 * cluster), and share it throughout your application;
		 *
		 * See https://docs.datastax.com/en/developer/java-driver/4.7/manual/core/configuration/
		 * 
		 * The default config is based on the Typesafe Config framework (https://github.com/typesafehub/config) :
		 *     the driver JAR comes with a reference.conf file that defines the defaults.
		 *     you can add an application.conf file in the classpath (or an absolute path, or an URL). It only needs to contain the options that you override.
		 *     hot reloading is supported out of the box.
		 *     
		 *     It looks at the following locations, according to the standard behavior of that library (first-listed are higher priority):
		 *         system properties
		 *         application.conf (all resources on the classpath with this name)
		 *         application.json (all resources on the classpath with this name)
		 *         application.properties (all resources on the classpath with this name)
		 *         reference.conf (all resources on the classpath with this name) - see https://docs.datastax.com/en/developer/java-driver/4.7/manual/core/configuration/reference/
		 *    
		 */
		
		
		File file = new File(cassandraConfigFileName);
		CqlSessionBuilder sessionBuilder = CqlSession.builder();
		if(file.canRead()) {
			LOG.info("Loading cassandra-application properties from {}", cassandraConfigFileName);
			sessionBuilder.withConfigLoader(DriverConfigLoader.fromFile(file));
		} else {
			LOG.info("Loading cassandra-application properties from classpath");			
			sessionBuilder.withConfigLoader(DriverConfigLoader.fromClasspath("cassandra-application"));
		}

		CqlSession session = sessionBuilder.build();
		
		session = new CqlSessionWithMetrics(session);
		
		return session;
	}

}
