/**
 * 
 */
package com.telecominfraproject.wlan.core.scheduler.models;

import com.telecominfraproject.wlan.core.model.scheduler.ImmutableTimeWindow;

/**
 * @author yongli
 *
 */
public interface ScheduledJob<I> {

    /**
     * Run the job. Method should check startTime against the timeWindows to
     * decide if it should proceed.
     * 
     * @param id
     * @param startTime
     *            - time when run is invoked
     * @param timeWindows
     *            - time windows when the job is schedule to run
     */
    public void runJob(final I id, long startTime, final ImmutableTimeWindow timeWindows);

    /**
     * Signal the job is cancelled.
     * 
     * @param id
     * @param startTime
     *            - time when run is invoked
     * @param immutableTimeWindow
     *            - time windows when the job is schedule to run
     */
    public void cancel(final I id, long startTime, final ImmutableTimeWindow immutableTimeWindow);

    /**
     * Test if the job is cancelled.
     * 
     * @return
     */
    public boolean isCancelled();
}
