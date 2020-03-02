package com.telecominfraproject.wlan.cloudmetrics.elasticsearch.client;

import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.telecominfraproject.wlan.core.model.service.ServiceInstanceInformation;
import com.telecominfraproject.wlan.elasticsearch.client.BaseElasticSearchStaticClientConfigurer;

@Configuration
@Profile("cloud-metrics-elastic-search")
public class ElasticSearchStaticClientConfigurer extends BaseElasticSearchStaticClientConfigurer {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchStaticClientConfigurer.class);

    @Autowired
    private ServiceInstanceInformation serviceInformation;

    @Autowired
    private Environment environment;

    public ElasticSearchStaticClientConfigurer() {
        super("whizcontrol.metricsElasticSearch.clusterName", "elasticsearch",
                "whizcontrol.metricsElasticSearch.hostNames", "localhost", null, DEFAULT_CLIENT_PORT);
    }

    @Bean
    public TransportClient cloudMetricsTransportClient() {
        return buildTransportClient(environment);
    }

    @Override
    public String getClientName() {
        return "CloudMetrics";
    }

    @Override
    protected Integer getDeploymentId() {
        return serviceInformation.getDeploymentId();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    // on shutdown
    // TODO: add shutdown hook?
    // client.close();
}
