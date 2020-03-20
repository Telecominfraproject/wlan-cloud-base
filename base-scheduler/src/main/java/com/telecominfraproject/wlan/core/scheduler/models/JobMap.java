/**
 * 
 */
package com.telecominfraproject.wlan.core.scheduler.models;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @param I
 *            - Id
 * @param J
 *            - Job
 * @author yongli
 *
 */
public class JobMap<I extends Comparable<I>> {
    public static class JobDetails<C extends Comparable<C>> {
        private final JobSchedule<C> schedule;
        private final ScheduledJob<C> job;

        public JobDetails(final JobSchedule<C> schedule, final ScheduledJob<C> job) {
            this.schedule = schedule;
            this.job = job;
        }

        public ScheduledJob<C> getJob() {
            return job;
        }

        public JobSchedule<C> getSchedule() {
            return schedule;
        }

        @Override
        public String toString() {
            return BaseJsonModel.toJsonString(this);
        }
    }

    private ConcurrentHashMap<I, JobDetails<I>> jobMap = new ConcurrentHashMap<>();

    /**
     * Use to determine the upcoming time
     */
    private ConcurrentSkipListSet<JobSchedule<I>> timerSet = new ConcurrentSkipListSet<>();

    public boolean addJob(JobSchedule<I> id, ScheduledJob<I> job) {
        JobDetails<I> jobDetails = new JobDetails<>(id, job);
        if (null == this.jobMap.putIfAbsent(id.getJobId(), jobDetails)) {
            if (!this.timerSet.add(id)) {
                this.jobMap.remove(id.getJobId(), jobDetails);
                return false;
            }
            return true;
        }
        return false;
    }

    @JsonIgnore
    public JobSchedule<I> getFirstTimer() {
        try {
            if (!timerSet.isEmpty()) {
                return timerSet.first();
            }
        } catch (NoSuchElementException exp) {
            // just in case it's removed after the isEmpty check
        }
        return null;
    }

    @JsonIgnore
    public int getSize() {
        return jobMap.size();
    }

    public JobDetails<I> removeJob(final I key) {
        JobDetails<I> details = this.jobMap.remove(key);
        if (null != details) {
            this.timerSet.remove(details.getSchedule());
        }
        return details;
    }

    public JobDetails<I> removeJob(final JobSchedule<I> schedule) {
        boolean result = this.timerSet.remove(schedule);
        if (result) {
            return this.jobMap.remove(schedule.getJobId());
        }
        return null;
    }

    @Override
    public final String toString() {
        return BaseJsonModel.toJsonString(this);
    }

    public JobMapStatus<I> getStatus() {
        return new JobMapStatus<>(getSize(), getFirstTimer());
    }
}
