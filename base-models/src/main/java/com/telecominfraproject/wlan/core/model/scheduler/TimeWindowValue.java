/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * Time windows with from and to time (epoch).
 * 
 * @author yongli
 *
 */
public class TimeWindowValue extends BaseJsonModel implements EpochTimeWindow, Comparable<TimeWindowValue> {
    private static final long serialVersionUID = 5732052015938478942L;
    private long beginTime;
    private long endTime;

    public TimeWindowValue(long beginTime, long endTime) {
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public TimeWindowValue(EpochTimeWindow value) {
        this(value.getBeginTime(), value.getEndTime());
    }

    protected TimeWindowValue() {
    }

    @Override
    public TimeWindowValue clone() {
        return (TimeWindowValue) super.clone();
    }

    @Override
    public int compareTo(TimeWindowValue other) {
        return EpochTimeWindow.compare(this, other);
    }

    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public boolean isAfter(long currentTime) {
        return EpochTimeWindow.isAfter(currentTime, this);
    }

    @Override
    public boolean isBefore(long currentTime) {
        return !isEmpty() && (currentTime < this.beginTime);
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return EpochTimeWindow.isEmpty(this);
    }

    @Override
    public boolean isWithin(long currentTime) {
        return EpochTimeWindow.isWithin(currentTime, this);
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * If value is null, return null. other-wise create a copy.
     * 
     * @param value
     * @return
     */
    public static TimeWindowValue create(final EpochTimeWindow value) {
        if (null == value) {
            return null;
        }
        return new TimeWindowValue(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginTime, endTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TimeWindowValue)) {
            return false;
        }
        TimeWindowValue other = (TimeWindowValue) obj;
        return this.beginTime == other.beginTime && this.endTime == other.endTime;
    }

}
