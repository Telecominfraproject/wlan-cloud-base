package com.telecominfraproject.wlan.partitioner;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class ServicePartitionerNodeInfo extends BaseJsonModel {
    private static final long serialVersionUID = -5007799853648354860L;
    private String nodeHostName;
    private Integer nodePort;
    private long startupTime;
    private int partitionId;

    public long getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(long startupTime) {
        this.startupTime = startupTime;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    public String getNodeHostName() {
        return nodeHostName;
    }

    public void setNodeHostName(String nodeHostName) {
        this.nodeHostName = nodeHostName;
    }

    public Integer getNodePort() {
        return nodePort;
    }

    public void setNodePort(Integer nodePort) {
        this.nodePort = nodePort;
    }

}
