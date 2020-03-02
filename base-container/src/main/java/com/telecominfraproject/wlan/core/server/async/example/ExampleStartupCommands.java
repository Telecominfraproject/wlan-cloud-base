package com.telecominfraproject.wlan.core.server.async.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author dtop
 * 
 * This is an example that shows that async methods are being executed by proper threads. 
 * It is invoked during container startup.
 *
 */
@Component
@Order(1)
@Profile(value={"asyncExample"})
public class ExampleStartupCommands implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleStartupCommands.class);

    @Autowired private AsyncCallerExample asyncCallerExample;

    public void run(String... args) {
        LOG.debug("Executing example on container startup");
        asyncCallerExample.exampleMethod();
    }

}
