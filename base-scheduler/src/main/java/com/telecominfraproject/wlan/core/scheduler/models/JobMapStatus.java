/**
 * 
 */
package com.telecominfraproject.wlan.core.scheduler.models;

/**
 * @author yongli
 *
 */
public class JobMapStatus<I extends Comparable<I>> {

    private final int size;
    private final JobSchedule<I> firstTimer;

    public JobMapStatus(int size, JobSchedule<I> firstTimer) {
        this.size = size;
        this.firstTimer = firstTimer;
    }

    public int getSize() {
        return size;
    }

    public JobSchedule<I> getFirstTimer() {
        return firstTimer;
    }

}
