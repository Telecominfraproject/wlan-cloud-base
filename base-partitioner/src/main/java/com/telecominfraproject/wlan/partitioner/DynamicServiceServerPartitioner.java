/**
 * 
 */
package com.telecominfraproject.wlan.partitioner;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * @author yongli
 *
 */
public class DynamicServiceServerPartitioner extends DynamicServicePartitioner
        implements ServicePartitionerServerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicServiceServerPartitioner.class);

    private final String nodeHostName;
    private final Integer nodePort;
    private final long nodeStartupTime;
    private volatile boolean nodeNeedsToRegister;

    public DynamicServiceServerPartitioner(String serviceName, String nodeHostName, Integer nodePort,
            HazelcastInstance hazelcastInstance, ApplicationContext applicationContext) {
        super(serviceName, hazelcastInstance);
        this.nodeHostName = nodeHostName;
        this.nodePort = nodePort;
        this.nodeStartupTime = applicationContext.getStartupDate();
    }

    @Override
    public void addCurrentNodeToTheCluster() {
        nodeNeedsToRegister = true;
        refreshCluster();
    }

    @Override
    protected void processNodeUrls(IMap<String, byte[]> partitionerMap) {
        if (nodeNeedsToRegister) {
            ServicePartitionerNodeInfo nodeInfo = new ServicePartitionerNodeInfo();
            nodeInfo.setNodeHostName(nodeHostName);
            nodeInfo.setNodePort(nodePort);
            nodeInfo.setStartupTime(nodeStartupTime);
            // compute partitionId from existing cluster info.

            // find index of the current node in the sorted nodeUrls
            int newPartition = clusterNodeUrls.indexOf("https://" + nodeHostName + ":" + nodePort);
            if (newPartition < 0) {
                // current node is not in the list yet. will add it, re-sort
                // and recalculate newPartition
                clusterNodeUrls.add("https://" + nodeHostName + ":" + nodePort);
                // re-sort
                clusterNodeUrls.sort(null);
                newPartition = clusterNodeUrls.indexOf("https://" + nodeHostName + ":" + nodePort);
                currentPartition = newPartition;
                LOG.info(
                        "In partitioned service {} registering current node {}:{} for partition {}. Total number of partitions {}",
                        serviceName, nodeHostName, nodePort, currentPartition, clusterNodeUrls.size());

            }

            if (newPartition != currentPartition) {
                LOG.info(
                        "In partitioned service {} current node {}:{} changed partition from {} to {}. Total number of partitions {}",
                        serviceName, nodeHostName, nodePort, currentPartition, newPartition, clusterNodeUrls.size());
                currentPartition = newPartition;
            }
            nodeInfo.setPartitionId(currentPartition);

            // update node registration record in the hazelcast
            partitionerMap.put(nodeInfo.getNodeHostName(), nodeInfo.toZippedBytes(), 10, TimeUnit.MINUTES);
        }
    }
}
