/**
 * 
 */
package com.telecominfraproject.wlan.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import com.telecominfraproject.wlan.remote.tests.BaseRemoteTest;

/**
 * Use for perform remote testing.
 * This class is used by the BaseRemoteTest to initialize a server for the remote interface testing.
 * <br>BaseRemoteTest by default configures  the value of the system property tip.wlan.componentScan.basePackages=com.telecominfraproject.wlan
 * <br>
 *  If your classes are located in a different subpackages and you want them to be scanned for the auto configuration, 
 *  you can set the system property to the list of subpackages to scan, for example:<br>
 *  <pre>
 *   static{  
 *      System.setProperty("tip.wlan.componentScan.basePackages", "com.telecominfraproject.wlan,com.netexperience");
 *   }
 *  </pre>
 *  <br>
 *  The code above can be placed into the static{} initializers in the descendants of the BaseRemoteTest  
 * 
 * @author dtop
 * @author yongli
 *
 */
@ComponentScan(basePackages = { "${tip.wlan.componentScan.basePackages}" })
@EnableAutoConfiguration
public class RemoteTestServer {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteTestServer.class);

    static {
        LOG.debug("configuring RemoteTestServer to scan {}", System.getProperty("tip.wlan.componentScan.basePackages"));        
    }
    
    public static void main(String[] args) {
        SpringApplication.run(RemoteTestServer.class, args);
    }
}
