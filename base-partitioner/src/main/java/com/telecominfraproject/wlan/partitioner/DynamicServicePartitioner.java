package com.telecominfraproject.wlan.partitioner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.map.IMap;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class DynamicServicePartitioner implements ServicePartitionerInterface {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicServicePartitioner.class);

    protected final String mapName;
    protected final String serviceName;
    protected int totalNumberOfPartitions;
    protected int currentPartition;
    protected final HazelcastInstance hazelcastInstance;
    private ClusterMonitorCallback clusterMonitorCallback;
    protected List<String> clusterNodeUrls = Collections.emptyList();

    private DynamicPartitionMonitorData monitorData;

    public DynamicServicePartitioner(String serviceName, HazelcastInstance hazelcastInstance) {
        this.serviceName = serviceName;
        this.mapName = this.serviceName + "_partitioner_map";
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public int getTotalNumberOfPartitions() {
        return totalNumberOfPartitions;
    }

    @Override
    public int getCurrentPartition() {
        return currentPartition;
    }

    @Override
    public void monitorCluster(ClusterMonitorCallback clusterMonitorCallback) {
        this.clusterMonitorCallback = clusterMonitorCallback;
    }

    @Override
    public List<String> getNodeUrls() {
        if (clusterNodeUrls == null || clusterNodeUrls.isEmpty()) {
            refreshCluster();
        }
        return clusterNodeUrls;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void refreshCluster() {
        IMap<String, byte[]> partitionerMap = hazelcastInstance.getMap(mapName);

        // lock the whole partitionerMap while refresh is happening
        // see https://docs.hazelcast.com/imdg/4.2/migration-guides
        CPSubsystem cpSubsystem = hazelcastInstance.getCPSubsystem();
        FencedLock mapLock = cpSubsystem.getLock("lock_" + mapName);
        mapLock.lock();

        try {
            clusterNodeUrls = new ArrayList<>();
            for (byte[] entryBytes : partitionerMap.values()) {
                ServicePartitionerNodeInfo ni = BaseJsonModel.fromZippedBytes(entryBytes,
                        ServicePartitionerNodeInfo.class);
                clusterNodeUrls.add("https://" + ni.getNodeHostName() + ":" + ni.getNodePort());
            }

            // sort the urls
            clusterNodeUrls.sort(null);

            processNodeUrls(partitionerMap);

            if (totalNumberOfPartitions != clusterNodeUrls.size()) {
                LOG.info("In partitioned service {} total number of partitions changed from {} to {}", serviceName,
                        totalNumberOfPartitions, clusterNodeUrls.size());
            }

            totalNumberOfPartitions = clusterNodeUrls.size();

        } finally {
            mapLock.unlock();
            monitorClusterStatus();
        }

    }

    /**
     * Do nothing
     * 
     * @param partitionerMap
     */
    protected void processNodeUrls(IMap<String, byte[]> partitionerMap) {
        // allow subclass to process the node
    }

    /**
     * Check the cluster.
     */
    private void monitorClusterStatus() {
        // make a copy in case it change
        ClusterMonitorCallback callback = clusterMonitorCallback;
        if (callback == null) {
            return;
        }
        synchronized (this) {
            if (this.monitorData == null) {
                this.monitorData = new DynamicPartitionMonitorData(this.serviceName);
            }
            this.monitorData.checkClusterInformation(clusterNodeUrls, callback);
        }
    }
}
