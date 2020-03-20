package com.telecominfraproject.wlan.partitioner;

import java.util.Collections;
import java.util.List;

/**
 * @author dtop
 *
 */
public class StaticServicePartitioner implements ServicePartitionerInterface {

    private final String serviceName;
    private final int totalNumberOfPartitions;
    private final int currentPartition;

    public StaticServicePartitioner(String serviceName) {
        this(serviceName, serviceName + ".numPartitions", serviceName + ".currentPartition");
    }

    public StaticServicePartitioner(String serviceName, String totalNumberOfPartitionsPropName,
            String currentPartitionPropName) {
        this.serviceName = serviceName;
        this.totalNumberOfPartitions = Integer.getInteger(totalNumberOfPartitionsPropName, 1);
        this.currentPartition = Integer.getInteger(currentPartitionPropName, 0);
    }

    @Override
    public int getTotalNumberOfPartitions() {
        return totalNumberOfPartitions;
    }

    @Override
    public int getCurrentPartition() {
        return currentPartition;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public List<String> getNodeUrls() {
        return Collections.emptyList();
    }

    @Override
    public void monitorCluster(ClusterMonitorCallback clusterMonitorCallback) {
        // DO NOTHING for now
    }

}
