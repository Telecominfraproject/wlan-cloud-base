/**
 * 
 */
package com.telecominfraproject.wlan.job;

/**
 * @author ekeddy
 *
 */
public interface NamedJob extends Runnable {

    String getJobName();
}
