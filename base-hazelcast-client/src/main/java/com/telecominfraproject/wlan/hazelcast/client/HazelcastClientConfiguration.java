package com.telecominfraproject.wlan.hazelcast.client;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.security.UsernamePasswordIdentityConfig;
import com.hazelcast.core.HazelcastInstance;
import com.telecominfraproject.wlan.core.model.utils.SystemAndEnvPropertyResolver;

/**
 * @author dtop
 *
 */
@Configuration
@Profile("use-hazelcast-client")
public class HazelcastClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastClientConfiguration.class);

    private static String defaultAwsRegion;

    @Autowired
    private Environment environment;

    //
    // hazelcast group security settings
    //
    @Value("${tip.wlan.hazelcast.groupName:wc-dev}")
    private String groupName;
    @Value("${tip.wlan.hazelcast.groupPassword:wc-dev-pass}")
    private String groupPassword;

    /**
     * comma-separated list of ipAddr:port of some of the nodes in the hazelcast
     * cluster
     */
    @Value("${tip.wlan.hazelcast.nodeAddresses:127.0.0.1:5701}")
    private String nodeAddressesStr;

    @Value("${tip.wlan.hazelcast.awsStackName:hcn01}")
    private String stackName;

    @Value("${tip.wlan.hazelcast.reconnectTimeSec:3600}")
    private int reconnectTimeSec;

    private String awsRegion;

    /**
     * Default constructor, used when Spring framework is active
     */
    public HazelcastClientConfiguration() {
    }

    @PostConstruct
    public void setupConfiguration() {
        this.awsRegion = environment.getProperty("tip.wlan.hazelcast.awsRegion");
    }

    /**
     * This constructor is for use outside of Spring framework, i.e. in EMR
     * Spark applications. This is configuration for AWS auto discovery
     * Hazelcast instance.
     *
     * @param groupName
     * @param groupPassword
     * @param stackName
     * @param awsRegion
     */
    public HazelcastClientConfiguration(String groupName, String groupPassword, String stackName, String awsRegion) {
        this.groupName = groupName;
        this.groupPassword = groupPassword;
        this.nodeAddressesStr = null;
        this.stackName = stackName;
        this.reconnectTimeSec = 3600;
        this.awsRegion = awsRegion;
    }

    /**
     * This constructor is for use outside of Spring framework, i.e. in EMR
     * Spark applications. This is configuration for unicast Hazelcast Instance
     *
     * @param groupName
     * @param groupPassword
     * @param nodeAddressesStr
     */
    public HazelcastClientConfiguration(String groupName, String groupPassword, String nodeAddressesStr) {
        this.groupName = groupName;
        this.groupPassword = groupPassword;
        this.nodeAddressesStr = nodeAddressesStr;
        this.reconnectTimeSec = 3600;
        this.stackName = null;
        this.awsRegion = null;
    }

    /**
     * @return HazelcastClientConfiguration constructed from System Properties
     *         or Environment Variables
     */
    public static HazelcastClientConfiguration createOutsideOfSpringApp() {
        String nodeAddressesStr = SystemAndEnvPropertyResolver
                .getPropertyAsString("tip.wlan.hazelcast.nodeAddresses", null);
        String groupPassword = SystemAndEnvPropertyResolver.getPropertyAsString("tip.wlan.hazelcast.groupPassword",
                "wc-dev-pass");
        String groupName = SystemAndEnvPropertyResolver.getPropertyAsString("tip.wlan.hazelcast.groupName",
                "wc-dev");
        if (nodeAddressesStr == null) {
            String stackName = SystemAndEnvPropertyResolver.getPropertyAsString("tip.wlan.hazelcast.awsStackName",
                    "hcn01");

            String awsRegion = SystemAndEnvPropertyResolver
                    .getPropertyAsString("tip.wlan.hazelcast.awsRegion", null);
            return new HazelcastClientConfiguration(groupName, groupPassword, stackName, awsRegion);
        }
        return new HazelcastClientConfiguration(groupName, groupPassword, nodeAddressesStr);
    }

    /**
     * Create HazelcastInstance based on the configuration. If
     * {@link #nodeAddressesStr} is not null, it will create use
     * {@link #hazelcastClientUnicast()}. Otherwise it will use
     * {@link #hazelcastInstanceAwsDiscovery()}
     *
     * @return created hazelcastInstance
     * @throws IOException
     */
    public HazelcastInstance createHazelcastInstance() throws IOException {
            return hazelcastClientUnicast();
    }

    @Profile("!hazelcast-aws-discovery")
    @Bean
    public HazelcastInstance hazelcastClientUnicast() {
        ClientConfig clientConfig = new ClientConfig();
//        clientConfig.setProperty(GroupProperty.LOGGING_TYPE.getName(), "slf4j");
//        clientConfig.setProperty(GroupProperty.PHONE_HOME_ENABLED.getName(), "false");

        clientConfig.getSecurityConfig().setUsernamePasswordIdentityConfig(new UsernamePasswordIdentityConfig(groupName, groupPassword));
        
        for (String addrStr : nodeAddressesStr.split(",")) {
            clientConfig.getNetworkConfig().addAddress(addrStr);
        }
        // see
        // http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of
        // the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        // the client will attempt to re-connect to the cluster forever if
        // cluster is not available
        //clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance client = new ReConnectingHazelcastClient(clientConfig, reconnectTimeSec);

        LOG.info("Configured Hazelcast client for cluster {}", nodeAddressesStr);

        return client;
    }


}
