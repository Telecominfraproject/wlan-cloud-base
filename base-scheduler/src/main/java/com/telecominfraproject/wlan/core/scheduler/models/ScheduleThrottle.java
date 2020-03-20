/**
 * 
 */
package com.telecominfraproject.wlan.core.scheduler.models;

/**
 * @author yongli
 *
 */
public interface ScheduleThrottle {
    /**
     * control how fast schedule pull job off the JobMap
     * 
     * @param currentTime
     *            when throttle is called
     * @return if paused, return value from
     *         {@linkplain System#currentTimeMillis()}
     */
    public long ready(long currentTime);
}
