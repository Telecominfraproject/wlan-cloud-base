/**
 * 
 */
package com.telecominfraproject.wlan.partitioner;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yongli
 *
 */
public class ClusterMonitorDetails {
    private final int currentSize;
    private final Integer normalSize;

    /**
     * Node URL which has been missing from previous monitor
     */
    private final Set<String> missingNodeUrls;

    /**
     * Node URL which is current running
     */
    private final Set<String> currentNodeUrls;

    public ClusterMonitorDetails(Integer normalSize, Set<String> previousNodeUrls, Set<String> nodeUrls) {
        this.normalSize = normalSize;
        this.currentNodeUrls = new TreeSet<>(nodeUrls);
        this.currentSize = this.getCurrentNodeUrls().size();
        if (previousNodeUrls != null) {
            missingNodeUrls = new TreeSet<>();
            for (String url : previousNodeUrls) {
                if (!getCurrentNodeUrls().contains(url)) {
                    getMissingNodeUrls().add(url);
                }
            }
        } else {
            missingNodeUrls = Collections.emptySet();
        }
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public Integer getNormalSize() {
        return normalSize;
    }

    public Set<String> getMissingNodeUrls() {
        return missingNodeUrls;
    }

    public Set<String> getCurrentNodeUrls() {
        return currentNodeUrls;
    }

}
