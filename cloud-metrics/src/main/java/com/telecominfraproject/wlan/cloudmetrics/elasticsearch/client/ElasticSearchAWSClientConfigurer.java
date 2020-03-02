/**
 * 
 */
package com.telecominfraproject.wlan.cloudmetrics.elasticsearch.client;

import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.service.ServiceInstanceInformation;
import com.telecominfraproject.wlan.elasticsearch.client.BaseElasticSearchAWSClientConfigurer;

/**
 * @author yongli
 *
 */
@Component
@Profile("cloud-metrics-aws-elastic-search")
public class ElasticSearchAWSClientConfigurer extends BaseElasticSearchAWSClientConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchAWSClientConfigurer.class);

    @Autowired
    private ServiceInstanceInformation serviceInformation;
    @Autowired
    private Environment environment;

    public ElasticSearchAWSClientConfigurer() {
        super("whizcontrol.metricsElasticSearch.clusterName", "elasticsearch", null, DEFAULT_CLIENT_PORT);
    }

    @Bean
    public TransportClient cloudMetricsTransportClient() {
        return buildTransportClient(environment);
    }

    @Override
    public String getClientName() {
        return "CloudMetrics";
    }
    // on shutdown
    // TODO: add shutdown hook?
    // client.close();

    /**
     * Monitor EC2 list every 10 minutes
     */
    @Scheduled(initialDelay = 10 * 60 * 1000, fixedDelay = 10 * 60 * 1000)
    public void monitorEC2HostNames() {
        super.monitorEC2HostNames(environment);
    }

    @Override
    protected Integer getDeploymentId() {
        return serviceInformation.getDeploymentId();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
