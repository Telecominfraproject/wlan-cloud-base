/**
 * 
 */
package com.whizcontrol.elasticsearch.client;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.core.env.Environment;

/**
 * @author yongli
 *
 */
public abstract class BaseElasticSearchStaticClientConfigurer extends BaseElasticSearchClientConfigurer {

    private String clusterHostNamesProperty;
    private String defaultHostNames;

    /**
     * Static host from porperty.
     * 
     * @param clusterNameProperty
     * @param defaultClusterName
     * @param clusterHostNamesProperty
     * @param defaultHostNames
     * @param defaultClientPort
     */
    protected BaseElasticSearchStaticClientConfigurer(final String clusterNameProperty, final String defaultClusterName,
            final String clusterHostNamesProperty, final String defaultHostNames, final String clientPortProperty,
            int defaultClientPort) {
        super(clusterNameProperty, defaultClusterName, clientPortProperty, defaultClientPort);
        this.clusterHostNamesProperty = clusterHostNamesProperty;
        this.defaultHostNames = defaultHostNames;
    }

    @Override
    protected Set<String> getHostForCluster(Environment environment) {
        Set<String> result = new TreeSet<>();
        String esHosts = getProperty(environment, clusterHostNamesProperty, defaultHostNames);
        for (String host : esHosts.split(",")) {
            result.add(host);
        }
        return result;
    }

}
