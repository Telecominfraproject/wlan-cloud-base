package com.telecominfraproject.wlan.partitioner;

import java.util.List;

/**
 * Service partitioner can be used on both server and client sides. When used on
 * the server side it maintains current partition and total number of
 * partitions, allows current node to be registered with the cluster and
 * periodically checks for cluster partition changes. When used on the client
 * side it maintains a list of all server node urls.
 * 
 * @author dtop
 *
 */
public interface ServicePartitionerInterface {

    int getTotalNumberOfPartitions();

    int getCurrentPartition();

    /**
     * This method is used by client side which needs to monitor the remote
     * cluster.
     * 
     * @param clusterMonitorCallback
     */
    void monitorCluster(ClusterMonitorCallback clusterMonitorCallback);

    /**
     * This method is mostly used by the client side of the service to route and
     * broadcast messages.
     * 
     * @return list of urls of all the nodes known to partitioned service
     */
    List<String> getNodeUrls();
}
