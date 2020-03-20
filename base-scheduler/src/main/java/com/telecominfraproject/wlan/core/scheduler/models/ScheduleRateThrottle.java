/**
 * 
 */
package com.telecominfraproject.wlan.core.scheduler.models;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.RateLimiter;

import ch.qos.logback.core.util.Duration;

/**
 * @author yongli
 *
 */
public class ScheduleRateThrottle implements ScheduleThrottle {
    private final RateLimiter impl;
    private final double permitsPerSecond;
    private final Duration warmupPeroid;

    /**
     * Use {@link RateLimiter} to provide throttle control.
     * 
     * See {@linkplain RateLimiter#create(double, long, TimeUnit)}
     * 
     * @param permitsPerSecond
     * @param warmupPeriod
     * @param unit
     */
    public ScheduleRateThrottle(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        this.permitsPerSecond = permitsPerSecond;
        this.warmupPeroid = Duration.buildByMilliseconds(unit.toMillis(warmupPeriod));
        this.impl = RateLimiter.create(permitsPerSecond, warmupPeriod, unit);
    }

    public double getPermitsPerSecond() {
        return permitsPerSecond;
    }

    public Duration getWarmupPeroid() {
        return warmupPeroid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.telecominfraproject.wlan.core.scheduler.models.ScheduleThrottle#ready(long)
     */
    @Override
    public long ready(long currentTime) {
        if (this.impl.tryAcquire()) {
            return System.currentTimeMillis();
        }
        return currentTime;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{\"_type\":\"").append(this.getClass().getSimpleName())
                .append("\",\"permitsPerSecond\":").append(this.permitsPerSecond).append(",\"warmupPeriod\":\"")
                .append(this.warmupPeroid).append("\"}").toString();
    }
}
