/**
 * 
 */
package com.telecominfraproject.wlan.elasticsearch.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * Configure TransportClient for Elasticsearch
 * 
 * @author yongli
 *
 */
public abstract class BaseElasticSearchClientConfigurer {
	
	private static final Logger LOG = LoggerFactory.getLogger(BaseElasticSearchClientConfigurer.class);
	
    /**
     * Default client port
     */
    public static final int DEFAULT_CLIENT_PORT = 9200;

    protected static int getProperty(Environment environment, String key, int defaultValue) {
        if (null == key) {
            return defaultValue;
        }
        if (environment != null) {
            return environment.getProperty(key, Integer.class, defaultValue);
        }
        return Integer.getInteger(key, defaultValue);
    }

    protected static String getProperty(Environment envornment, String key, String defaultValue) {
        if (null == key) {
            return defaultValue;
        }
        if (null != envornment) {
            return envornment.getProperty(key, defaultValue);
        }
        return System.getProperty(key, defaultValue);
    }

    /**
     * ES cluster name
     */
    private String clusterName;
    /**
     * Property name for populating cluster name
     */
    private final String clusterNameProperty;
    /**
     * Default cluster name
     */
    private final String defaultClusterName;

    /**
     * The elasticsearch client
     */
    private RestHighLevelClient client;
    /**
     * Client port
     */
    private int clientPort;
    /**
     * Property name for populating client port
     */
    private final String clientPortProperty;

    /**
     * Default client port
     */
    private final int defaultClientPort;

    /**
     * 
     * @param clusterNameProperty
     * @param defaultClusterName
     * @param clientPortProperty
     * @param defaultClientPort
     */
    protected BaseElasticSearchClientConfigurer(final String clusterNameProperty, final String defaultClusterName,
            final String clientPortProperty, int defaultClientPort) {
        this.clusterNameProperty = clusterNameProperty;
        this.defaultClusterName = defaultClusterName;
        this.clientPortProperty = clientPortProperty;
        this.defaultClientPort = defaultClientPort;
        this.clientPort = DEFAULT_CLIENT_PORT;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    protected RestHighLevelClient buildTransportClient(Environment environment) {
        getLogger().info("Configuring ElasticSearchClient for {}", getClientName());
        if (null != this.client) {
            return this.client;
        }
        // see
        // https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high.html

        // on startup
        // discover all nodes in cluster
        if (null == this.clusterName) {
            this.clusterName = getProperty(environment, clusterNameProperty, defaultClusterName);
        }
        this.clientPort = getProperty(environment, clientPortProperty, defaultClientPort);
        
        List<HttpHost>  httpHosts = new ArrayList<>();
        		
        Set<String> addressList = getHostForCluster(environment);
        if (addressList.isEmpty()) {
            getLogger().error("No host address found for {} cluster {}", getClientName(), clusterName);
            throw new ConfigurationException(
                    "No host address found for " + getClientName() + " cluster " + clusterName);
        }

        for (String address : addressList) {
            try {
                httpHosts.add(new HttpHost(InetAddress.getByName(address), clientPort, "http"));
                getLogger().info("Added transport address {} to {}({})", address, getClientName(), this.clusterName);
            } catch (UnknownHostException e) {
                getLogger().error("Invalid host address specified for {} ElasticSearchClient {}: {}", getClientName(),
                        address, e.getLocalizedMessage());
                throw new ConfigurationException(
                        "Invalid host address specified for " + getClientName() + " ElasticSearchClient " + address, e);
            }
            knownHostsAdded(addressList);
        }
        
        
		RestClientBuilder restClientBuilder = RestClient.builder( httpHosts.toArray(new HttpHost[0]) );
		this.client = new RestHighLevelClient(restClientBuilder);
		

        return this.client;
    }

    public abstract String getClientName();

    protected RestHighLevelClient getClient() {
        return client;
    }

    protected int getClientPort() {
        return this.clientPort;
    }

    /**
     * Get the current deploymentId
     * 
     * @return
     */
    protected abstract Integer getDeploymentId();

    /**
     * Return the list of host name for the ES data nodes.
     * 
     * @param environment
     * 
     * @return
     */
    protected abstract Set<String> getHostForCluster(Environment environment);

    /**
     * Get the logger for logging
     * 
     * @return
     */
    protected abstract Logger getLogger();

    /**
     * list of known data node hosts added when building the client.
     * 
     * @param addressList
     */
    protected void knownHostsAdded(Set<String> addressList) {
    }

    /**
     * on shutdown
     */
    protected void shutdownClient() {
        // TODO: add shutdown hook?
        try {
			client.close();
		} catch (IOException e) {
			LOG.debug("Exception when closing elasticsearch client", e);
		}
        
        client = null;
    }
}
