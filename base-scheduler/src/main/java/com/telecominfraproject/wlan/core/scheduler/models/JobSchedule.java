package com.telecominfraproject.wlan.core.scheduler.models;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.scheduler.ImmutableTimeWindow;

public class JobSchedule<T extends Comparable<T>> implements Comparable<JobSchedule<T>> {
    private final T jobId;
    private final ImmutableTimeWindow timeWindows;

    public JobSchedule(T jobId, ImmutableTimeWindow timeWindows) {
        this.jobId = jobId;
        this.timeWindows = timeWindows;
    }

    /**
     * Compare {@link #timeWindows} first, the {@link #jobId}.
     * 
     * Use to determine the upcoming schedule.
     */
    @Override
    public int compareTo(JobSchedule<T> other) {
        int result = timeWindows.compareTo(other.timeWindows);
        if (0 == result) {
            return jobId.compareTo(other.jobId);
        }
        return result;
    }

    public T getJobId() {
        return jobId;
    }

    @JsonIgnore
    public ImmutableTimeWindow getTimeWindows() {
        return timeWindows;
    }

    public String getTimeWindowsDetails() {
        return (null == timeWindows) ? null : timeWindows.toString();
    }

    @Override
    public String toString() {
        return BaseJsonModel.toJsonString(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, timeWindows);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JobSchedule)) {
            return false;
        }
        JobSchedule other = (JobSchedule) obj;
        return Objects.equals(jobId, other.jobId) && Objects.equals(timeWindows, other.timeWindows);
    }
    
}
