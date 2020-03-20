package com.telecominfraproject.wlan.partitioner;

public interface ServicePartitionerServerInterface extends ServicePartitionerInterface {

    /**
     * This method is used by the server side of the service to register itself
     * with the cluster.
     */
    void addCurrentNodeToTheCluster();
}
