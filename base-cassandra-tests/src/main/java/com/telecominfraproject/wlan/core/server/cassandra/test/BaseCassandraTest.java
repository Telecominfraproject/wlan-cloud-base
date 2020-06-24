package com.telecominfraproject.wlan.core.server.cassandra.test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.telecominfraproject.wlan.core.server.cassandra.BaseCassandraDataSource;

/**
 * Base test classes for Cassandra DAOs. 
 *  
 * @author dtop
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = {BaseCassandraTest.Config.class, BaseCassandraDataSource.class})
public abstract class BaseCassandraTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseCassandraTest.class);

    @Configuration
    public static class Config {
        // Put all required @Bean -s in here - they will be injected into the
        // AppIntegrationTest class

    }

}
