package com.telecominfraproject.wlan.core.server.controller.ping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.telecominfraproject.wlan.core.server.container.CommitProperties;

import com.telecominfraproject.wlan.core.server.container.ConnectorProperties;


/**
 * @author dtoptygin
 *
 */
@Controller
public class PingController {

    private static final Logger LOG = LoggerFactory.getLogger(PingController.class);
    @Autowired private ApplicationContext applicationContext;
    @Autowired private ConnectorProperties connectorProperties;
    @Autowired private CommitProperties commitProperties;


    
    @GetMapping("/ping")
    public @ResponseBody PingResponse ping() {
        PingResponse ret = new PingResponse(
                applicationContext.getStartupDate(), 
                System.currentTimeMillis(), 
                applicationContext.getEnvironment().getProperty("app.name"),
                applicationContext.getEnvironment().getProperty("elb", connectorProperties.getExternalHostName()),
                commitProperties.getCommitID(),
                commitProperties.getCommitDate(),
                commitProperties.getProjectVersion()
                );
        		
        LOG.debug("ping: {} / {}", ret.getStartupTime(), ret.getCurrentTime());

        return ret;
    }
    

}
