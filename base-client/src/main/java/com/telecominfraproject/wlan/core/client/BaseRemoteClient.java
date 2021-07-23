package com.telecominfraproject.wlan.core.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestOperations;

import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * @author dtop
 *
 */
public abstract class BaseRemoteClient {

    @Autowired protected Environment environment;

    protected RestOperations restTemplate;
    protected HttpHeaders headers = new HttpHeaders();

    {
        // Note: APPLICATION_JSON_UTF8 is deprecated
        headers.setContentType(MediaType.APPLICATION_JSON);
        //Accept-Encoding: gzip,deflate
        headers.set("Accept-Encoding", "gzip,deflate");
    }
    
    @Autowired
    private void setRestTemplate(RestOperations restTemplate) {
        //build user-friendly metrics Id - remove $$EnhancedByCGlib... at the end of the class name
        String metricsId = this.getClass().getSimpleName();
        int idx = metricsId.indexOf('$');
        if(idx>0){
            metricsId = metricsId.substring(0, idx);
        }
        this.restTemplate = new RestOperationsWithMetrics(restTemplate, metricsId);
    }
    
    protected String getBaseUrlFromEnvironment(String urlPropName, String defaultUrlPropName) {
        String value = environment.getProperty(urlPropName);
        if (value == null && defaultUrlPropName != null) {
            value = environment.getProperty(defaultUrlPropName);
        } else if (value == null) {
            throw new ConfigurationException(
                    "Could not find environment property for '" + urlPropName + "' and no default property is defined");
        }

        if (value == null) {
            throw new ConfigurationException(
                    "Could not find environment property for '" + urlPropName + "' and '" + defaultUrlPropName + "'");
        }
        return value.trim();
    }
    
    protected String getBaseUrlFromSystem(String urlPropName, String defaultUrlPropName) {
        String value = System.getProperty(urlPropName);
        if (value == null && defaultUrlPropName != null) {
            value = System.getProperty(defaultUrlPropName);
        } else if (value == null) {
            throw new ConfigurationException(
                    "Could not find system property for '" + urlPropName + "' and no default property is defined");
        }

        if (value == null) {
            throw new ConfigurationException(
                    "Could not find system property for '" + urlPropName + "' and '" + defaultUrlPropName + "'");
        }
        return value.trim();
    }



}
