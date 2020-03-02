/**
 * 
 */
package com.telecominfraproject.wlan.elasticsearch.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.springframework.core.env.Environment;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * @author yongli
 *
 */
public abstract class BaseElasticSearchAWSClientConfigurer extends BaseElasticSearchClientConfigurer {
    private static final String TAG_CF_STACK_NAME = "tag:aws:cloudformation:stack-name";
    private static final String TAG_CF_DEP_ID = "tag:a2w:cloudformation:deployment";
    private static final String TAG_CF_NODE_TYPE = "tag:a2w:cloudformation:nodeType";
    private static final String TAG_VALUE_DATA_NODE = "dataNode";
    private Set<String> lastKnownHosts = Collections.synchronizedSet(new TreeSet<>());

    /**
     * Setup the cluster name both for AWS and ES.
     * 
     * @param clusterNameProperty
     * @param defaultClusterName
     * @param clientPortProperty
     * @param clientPort
     */
    protected BaseElasticSearchAWSClientConfigurer(final String clusterNameProperty, final String defaultClusterName,
            final String clientPortProperty, int defaultClientPort) {
        super(clusterNameProperty, defaultClusterName, clientPortProperty, defaultClientPort);
    }

    @Override
    protected Set<String> getHostForCluster(Environment environment) {
        Set<String> result = new TreeSet<>();
        AmazonEC2 ec2Client = AmazonEC2ClientBuilder.defaultClient();

        // "Name=tag:aws:cloudformation:stack-name,Values=clusterName"
        Filter stackNameFilter = new Filter(TAG_CF_STACK_NAME, Arrays.asList(getClusterName()));
        // "Name=tag:a2w:cloudformation:deployment,Values=deployId"
        Filter deployIdFilter = new Filter(TAG_CF_DEP_ID, Arrays.asList(Integer.toString(getDeploymentId())));
        // "Name=tag:a2w:cloudformation:nodeType,Values=dataNode"
        Filter dataNodeFilter = new Filter(TAG_CF_NODE_TYPE, Arrays.asList(TAG_VALUE_DATA_NODE));
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setFilters(Arrays.asList(stackNameFilter, deployIdFilter, dataNodeFilter));

        DescribeInstancesResult instancesResult = ec2Client.describeInstances(request);
        if (null != instancesResult) {
            for (Reservation reservation : instancesResult.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    result.add(instance.getPrivateDnsName());
                }
            }
        }
        return result;
    }

    @Override
    protected void knownHostsAdded(Set<String> addressList) {
        lastKnownHosts.addAll(addressList);
    }

    /**
     * Subclass should schedule this at a fixed interval.
     * 
     * @param environment
     */
    protected void monitorEC2HostNames(Environment environment) {
        if (null == getClient()) {
            return;
        }
        Set<String> addressList = getHostForCluster(environment);
        if (addressList.isEmpty()) {
            getLogger().warn("No EC2 node found for ElasticSearch({})", getClusterName());
            return;
        }

        if (this.lastKnownHosts.equals(addressList)) {
            return;
        }
        for (String address : addressList) {
            if (this.lastKnownHosts.contains(address)) {
                continue;
            }
            try {
                getClient().addTransportAddress(
                        new InetSocketTransportAddress(InetAddress.getByName(address), getClientPort()));
                getLogger().info("Added transport address {} to {}({})", getClientName(), address, getClusterName());
            } catch (UnknownHostException e) {
                getLogger().error("Invalid host address {} found for {}({}): {}", address, getClientName(),
                        getClusterName(), e.getLocalizedMessage());
            }
            this.lastKnownHosts.add(address);
        }

        // now find stale one from lastKnowHosts
        Set<String> removeSet = new TreeSet<>();
        for (String address : this.lastKnownHosts) {
            if (addressList.contains(address)) {
                continue;
            }
            removeSet.add(address);
        }

        // locate host to remove
        List<TransportAddress> removeAddresses = new ArrayList<>();
        for (TransportAddress address : getClient().transportAddresses()) {
            if (null == address.getHost()) {
                // this could be auto discovered node which has no host
                continue;
            }
            if (removeSet.contains(address.getHost())) {
                removeAddresses.add(address);
            }
        }

        for (TransportAddress address : removeAddresses) {
            getLogger().info("Removing transport address {} from {}({})", address.getHost(), getClientName(),
                    getClusterName());
            this.lastKnownHosts.remove(address.getHost());
            getClient().removeTransportAddress(address);
        }
    }
}
