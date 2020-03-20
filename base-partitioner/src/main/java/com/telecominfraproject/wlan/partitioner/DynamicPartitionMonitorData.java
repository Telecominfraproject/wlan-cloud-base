/**
 * 
 */
package com.telecominfraproject.wlan.partitioner;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author yongli
 *
 */
public class DynamicPartitionMonitorData {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicPartitionMonitorData.class);
    private final String serviceName;
    private Instant lastResetTime;
    private Set<String> lastNodeUrl;
    private int normalClusterSize;
    private boolean issueRaised;

    public DynamicPartitionMonitorData(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * We will raise issue when
     * <ul>
     * <li>Size reduced from normal
     * <li>Size reached zero
     * </ul>
     * 
     * We will clear alarm when
     * <ul>
     * <li>Size return to normal
     * </ul>
     * 
     * Normal size is reset to max non-zero partition count since the beginning
     * of the hour.
     * 
     * @param clusterNodeUrls
     * @param callback
     */
    public synchronized void checkClusterInformation(List<String> clusterNodeUrls, ClusterMonitorCallback callback) {
        LOG.debug("Cluster monitor start for {}: last normal size {}, last nodes {}, current nodes {}", this.serviceName,
                this.normalClusterSize, this.lastNodeUrl, clusterNodeUrls);
        Instant currentTime = Instant.now();
        if (lastResetTime == null) {
            this.lastResetTime = currentTime;
            this.lastNodeUrl = new TreeSet<>(clusterNodeUrls);
            this.normalClusterSize = this.lastNodeUrl.size();

            // raise issue if clsuter size is empty
            if ((0 == this.normalClusterSize) && !this.issueRaised) {
                LOG.warn("Raise cluster issue for {}, empty cluster", this.serviceName);
                this.issueRaised = callback.raiseIssue(this.serviceName,
                        BaseJsonModel.toPrettyJsonString(new ClusterMonitorDetails(null, null, lastNodeUrl)));
            }
            return;
        }

        Set<String> currentNodeUrl = new TreeSet<>(clusterNodeUrls);
        if (needResetNormal(currentTime)) {
            if (!currentNodeUrl.isEmpty()) {
                // only reset if current size is not empty
                this.normalClusterSize = currentNodeUrl.size();
                this.lastResetTime = currentTime;
            }
        } else if (currentNodeUrl.size() > this.normalClusterSize) {
            this.normalClusterSize = currentNodeUrl.size();
            this.lastResetTime = currentTime;
        }

        if (currentNodeUrl.size() < this.normalClusterSize) {
            // size reduced
            if (!issueRaised) {
                LOG.warn("Raise cluster issue for {}, cluster size below normal size {}: {}", this.serviceName,
                        this.normalClusterSize, currentNodeUrl);
                this.issueRaised = callback.raiseIssue(this.serviceName,
                        BaseJsonModel.toPrettyJsonString(
                                new ClusterMonitorDetails((0 == this.normalClusterSize) ? null : this.normalClusterSize,
                                        this.lastNodeUrl, currentNodeUrl)));
            }
        } else {
            if (!issueRaised && currentNodeUrl.isEmpty()) {
                LOG.warn("Raise cluster issue for {}, empty cluster size below normal size {}", this.serviceName,
                        this.normalClusterSize);
                this.issueRaised = callback.raiseIssue(this.serviceName,
                        BaseJsonModel.toPrettyJsonString(
                                new ClusterMonitorDetails((0 == this.normalClusterSize) ? null : this.normalClusterSize,
                                        this.lastNodeUrl, currentNodeUrl)));
            } else if (issueRaised) {
                // normal, check if we need to clear issue
                LOG.info("Clear cluster issue for {}, cluster size restored to normal size {}: {}", this.serviceName,
                        this.normalClusterSize, currentNodeUrl);
                this.issueRaised = !callback.clearIssue(this.serviceName,
                        BaseJsonModel.toPrettyJsonString(
                                new ClusterMonitorDetails((0 == this.normalClusterSize) ? null : this.normalClusterSize,
                                        this.lastNodeUrl, currentNodeUrl)));
            }
        }
        this.lastNodeUrl = currentNodeUrl;

        LOG.debug("Cluster monitor ended for {}: normal size {}, current nodes {}", this.serviceName,
                this.normalClusterSize, this.lastNodeUrl);
    }

    /**
     * Check if currentTime crossed hour boundary of the {@link #lastResetTime}
     * 
     * @param currentTime
     * @return true if hour boundary crossed
     */
    private boolean needResetNormal(Instant currentTime) {
        LocalDateTime lastResetDate = LocalDateTime.ofInstant(this.lastResetTime, ZoneOffset.UTC);
        LocalDateTime currentDate = LocalDateTime.ofInstant(currentTime, ZoneOffset.UTC);

        return (currentDate.isAfter(lastResetDate) && (lastResetDate.getHour() != currentDate.getHour()));
    }

}
