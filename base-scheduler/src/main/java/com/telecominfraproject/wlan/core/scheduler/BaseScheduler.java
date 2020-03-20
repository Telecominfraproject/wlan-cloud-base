/**
 * 
 */
package com.telecominfraproject.wlan.core.scheduler;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.scheduler.TimeWindowValue;
import com.telecominfraproject.wlan.core.scheduler.models.JobMap;
import com.telecominfraproject.wlan.core.scheduler.models.JobMapStatus;
import com.telecominfraproject.wlan.core.scheduler.models.JobSchedule;
import com.telecominfraproject.wlan.core.scheduler.models.ScheduleThrottle;
import com.telecominfraproject.wlan.core.scheduler.models.ScheduledJob;
import com.telecominfraproject.wlan.core.scheduler.models.JobMap.JobDetails;

/**
 * Schedule Service
 * 
 * @param T
 *            Job Id
 * 
 * @author yongli
 *
 */
public abstract class BaseScheduler<I extends Comparable<I>> {

    /**
     * Frequency to check the {@link #jobMap}
     */
    private static final long MAX_SLEEP_TIME_MS = TimeUnit.MINUTES.toMillis(1);

    private boolean endScheduler = false;
    /**
     * Map of scheduled job
     */
    private final JobMap<I> jobMap = new JobMap<>();
    private final Logger Logger;
    private Thread runThread;
    private boolean startScheduler = false;

    private final ScheduleThrottle throttle;

    public BaseScheduler(Logger Logger, final ScheduleThrottle throttle) {
        if (null != Logger) {
            this.Logger = Logger;
        } else {
            this.Logger = LoggerFactory.getLogger(getClass());
        }
        this.throttle = throttle;
    }

    /**
     * Cancel a job. It will invoke {@linkplain ScheduledJob#cancel()}
     * 
     * @param jobId
     */
    public void cancelJob(final I jobId) {
        Logger.debug("cancelJob({})", jobId);
        if (null == jobId) {
            return;
        }
        JobDetails<I> jobDetails = this.jobMap.removeJob(jobId);
        if (null != jobDetails) {
            jobDetails.getJob().cancel(jobDetails.getSchedule().getJobId(), System.currentTimeMillis(),
                    jobDetails.getSchedule().getTimeWindows());
        }
        Logger.debug("cancelJob({}) returns", jobId);
    }

    /**
     * Get the name of the scheduler
     * 
     * @return
     */
    abstract public String getName();

    /**
     * Get the job map status
     * 
     * @return
     */
    public JobMapStatus<I> getStatus() {
        return this.jobMap.getStatus();
    }

    public ScheduleThrottle getThrottle() {
        return throttle;
    }

    /**
     * Check if schedule thread is running.
     * 
     * @return
     */
    public boolean isRunning() {
        Thread thread = this.runThread;
        return (null != thread) && thread.isAlive();
    }

    public boolean shutdownScheduler(long timeout) {
        synchronized (this) {
            if (!startScheduler) {
                throw new IllegalStateException("Scheduler not running");
            }
            endScheduler = true;
        }
        long waitTill = System.currentTimeMillis() + timeout;

        if (null != this.runThread) {
            for (long currentTime = System.currentTimeMillis(); currentTime < waitTill; currentTime = System
                    .currentTimeMillis()) {
                if (null != this.runThread) {
                    if (this.runThread.isAlive()) {
                        synchronized (this.jobMap) {
                            this.jobMap.notifyAll();
                        }
                    } else {
                        this.runThread = null;
                    }
                }

                synchronized (this) {
                    try {
                        if (null == this.runThread) {
                            this.startScheduler = false;
                            break;
                        }
                        this.wait(TimeUnit.SECONDS.toMillis(1));
                    } catch (InterruptedException e) {
                        // do nothing
                        Thread.currentThread().interrupt();
                    }
                }

            }
        }
        return this.startScheduler;
    }

    /**
     * Start the scheduler run thread
     * 
     * @param timeout
     * 
     * @return true if thread is started
     */
    public boolean startScheduler(long timeout) {
        synchronized (this) {
            if (null != this.runThread) {
                throw new IllegalStateException("Run thread already started");
            }

            this.runThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runScheduleLoopUntilExit();
                }
            });
        }
        this.runThread.setDaemon(true);
        this.runThread.setName(getName());
        long waitTill = System.currentTimeMillis() + timeout;
        this.runThread.start();

        // wait for start signal
        for (long currentTime = System.currentTimeMillis(); currentTime < waitTill; currentTime = System
                .currentTimeMillis()) {
            synchronized (this) {
                if (startScheduler) {
                    break;
                }
                try {
                    this.wait(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException exp) {
                    // ignore
                    Thread.currentThread().interrupt();
                }
            }
        }
        return startScheduler;
    }

    /**
     * Submit a job to run at the time window.
     * 
     * When the time come, it will enqueue the job and then run the
     * {@linkplain ScheduledJob#runJob(Object, long, TimeWindowValue)}
     * 
     * @throws IllegalArgumentException
     *             if schedule has empty time window
     * @throws IllegalStateException
     *             if scheduler is not running
     * @param schedule
     * @param job
     */
    public void submitJob(final JobSchedule<I> schedule, final ScheduledJob<I> job) {
        if (null == this.runThread) {
            throw new IllegalStateException("Schedule is not running");
        }
        if (schedule.getTimeWindows().isEmpty()) {
            throw new IllegalArgumentException("Empty time window in " + schedule);
        }
        this.Logger.debug("submitJob({},{})", schedule, job);
        this.jobMap.addJob(schedule, job);
        synchronized (this.jobMap) {
            this.jobMap.notifyAll();
        }
    }

    @Override
    final public String toString() {
        return BaseJsonModel.toJsonString(this);
    }

    /**
     * Subclass should run this in a fixed timer interval. It returns the
     * {@linkplain TimeWindowValue#getBeginTime()} of the first job in the
     * queue.
     * 
     * @param currentTime
     * @return begin time for the first job in the queue
     */
    private Long runScheduledJob(long currentTime) {
        Long firstStartup = null;
        JobSchedule<I> timer = jobMap.getFirstTimer();
        if (null != timer) {
            firstStartup = timer.getTimeWindows().getBeginTime();
            if (timer.getTimeWindows().isWithin(currentTime)) {
                JobDetails<I> jobDetails = jobMap.removeJob(timer);
                if (null != jobDetails) {
                    try {
                        enqueuJob(jobDetails);
                    } catch (Exception exp) {
                        getLogger().error("enqueuJob({}) failed due to {} exception {}", jobDetails,
                                exp.getClass().getSimpleName(), exp.getLocalizedMessage());
                    }
                }
            }
        }
        return firstStartup;
    }

    /**
     * Run this in a dedicated thread
     */
    private void runScheduleLoopUntilExit() {
        Logger.info("Starting scheduler run loop thread");
        synchronized (this) {
            if (startScheduler) {
                throw new IllegalStateException("Scheduler already started running");
            }
            startScheduler = true;
            endScheduler = false;
            this.notifyAll();
        }
        try {
            while (!this.endScheduler) {
                try {
                    long currentTime = System.currentTimeMillis();
                    if (null != this.getThrottle()) {
                        currentTime = this.getThrottle().ready(currentTime);
                    }
                    Long nextStartupTime = runScheduledJob(currentTime);
                    long waitTime = MAX_SLEEP_TIME_MS;
                    if (null != nextStartupTime) {
                        currentTime = System.currentTimeMillis();
                        if (nextStartupTime <= currentTime) {
                            waitTime = 0;
                        } else if (nextStartupTime < currentTime + MAX_SLEEP_TIME_MS) {
                            waitTime = nextStartupTime - currentTime;
                        }
                    }
                    if (waitTime > 0) {
                        synchronized (this.jobMap) {
                            if (!this.endScheduler) {
                                this.jobMap.wait(waitTime);
                            }
                        }
                    }
                } catch (InterruptedException exp) {
                    // ignore it
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            synchronized (this) {
                if (startScheduler) {
                    startScheduler = false;
                }
            }
            Logger.info("Existing scheduler run loop thread");
        }
    }

    /**
     * Sub-class should remove all the job enqueued and call cancel
     */
    abstract protected void emptyJobQueue();

    /**
     * Sub-class should enqueue the job and run it
     * 
     * @param jobDetails
     */
    abstract protected void enqueuJob(JobDetails<I> jobDetails);

    @JsonIgnore
    protected Logger getLogger() {
        return Logger;
    }
}
