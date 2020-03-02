/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.Instant;

/**
 * Time window that's immutable.
 * 
 * @author yongli
 *
 */
public class ImmutableTimeWindow implements EpochTimeWindow, Comparable<ImmutableTimeWindow> {
    private static final String EMPTY_TIME_WINDOW = "[Empty]";
    private final long beginTime;
    private final long endTime;

    public ImmutableTimeWindow(long fromTime, long toTime) {
        this.beginTime = fromTime;
        this.endTime = toTime;
    }

    public ImmutableTimeWindow(final EpochTimeWindow data) {
        this(data.getBeginTime(), data.getEndTime());
    }

    @Override
    public int compareTo(final ImmutableTimeWindow other) {
        return EpochTimeWindow.compare(this, other);
    }

    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public boolean isWithin(long currentTime) {
        return EpochTimeWindow.isWithin(currentTime, this);
    }

    @Override
    public boolean isEmpty() {
        return EpochTimeWindow.isEmpty(this);
    }

    @Override
    public boolean isBefore(long currentTime) {
        return !isEmpty() && (currentTime < this.beginTime);
    }

    @Override
    public boolean isAfter(long currentTime) {
        return EpochTimeWindow.isAfter(currentTime, this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.beginTime ^ (this.beginTime >>> 32));
        result = prime * result + (int) (this.endTime ^ (this.endTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ImmutableTimeWindow)) {
            return false;
        }
        ImmutableTimeWindow other = (ImmutableTimeWindow) obj;
        if (this.beginTime != other.beginTime) {
            return false;
        }
        if (this.endTime != other.endTime) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return EMPTY_TIME_WINDOW;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(Instant.ofEpochMilli(this.beginTime)).append('(').append(this.beginTime).append(")-")
                .append(Instant.ofEpochMilli(this.endTime)).append('(').append(this.endTime).append(")]");
        return sb.toString();
    }
}
