/**
 * 
 */
package com.telecominfraproject.wlan.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Use for perform remote testing.
 * 
 * @author yongli
 *
 */
@ComponentScan(basePackages = { "com.whizcontrol" })
@EnableAutoConfiguration
public class RemoteTestServer {
    public static void main(String[] args) {
        SpringApplication.run(RemoteTestServer.class, args);
    }
}
