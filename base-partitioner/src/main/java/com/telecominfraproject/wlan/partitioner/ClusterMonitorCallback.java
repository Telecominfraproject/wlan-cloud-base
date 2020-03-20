/**
 * 
 */
package com.telecominfraproject.wlan.partitioner;

/**
 * Callback used when monitor cluster
 * 
 * @author yongli
 *
 */
public interface ClusterMonitorCallback {
    /**
     * Raise an issue
     * 
     * @param serviceName
     * @param issueDetails
     * 
     * @return issue raised
     */
    boolean raiseIssue(String serviceName, String issueDetails);

    /**
     * Clear an issue
     * 
     * @param serviceName
     * @param issueDetails
     * 
     * @return issue cleared
     */
    boolean clearIssue(String serviceName, String issueDetails);
}
