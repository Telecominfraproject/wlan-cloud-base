/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

/**
 * Time window with begin and end time in EPOCH value.
 * 
 * @author yongli
 *
 */
public interface EpochTimeWindow {

    /**
     * Test if the current time has past the {@link #getEndTime()} in value.
     * 
     * @param currentTime
     * @param value
     * @return always return false for empty value
     */
    static boolean isAfter(long currentTime, final EpochTimeWindow value) {
        return !isEmpty(value) && (currentTime > value.getEndTime());
    }

    /**
     * Test if current time is before the {@link #getBeginTime()} in value.
     * 
     * @param currentTime
     * @param value
     * @return always return false for empty value
     */
    static boolean isBefore(long currentTime, final EpochTimeWindow value) {
        return !isEmpty(value) && (currentTime < value.getBeginTime());
    }

    /**
     * Test if the value is null or it has valid duration between
     * {@link #getBeginTime()} and {@link #getEndTime()}
     * 
     * @param value
     * @return boolean
     */
    static boolean isEmpty(final EpochTimeWindow value) {
        return (null == value) || (value.getBeginTime() >= value.getEndTime());
    }

    /**
     * Test if current time is within the {@link #getBeginTime()} and
     * {@link #getEndTime()} inclusively in value.
     * 
     * @param currentTime
     * @param value
     * @return always return false for empty value
     */
    static boolean isWithin(long currentTime, final EpochTimeWindow value) {
        return !isEmpty(value) && (currentTime >= value.getBeginTime()) && (currentTime <= value.getEndTime());
    }

    long getBeginTime();

    long getEndTime();

    /**
     * See {@link #isAfter(long, EpochTimeWindow)}
     * 
     * @param currentTime
     * @return boolean
     */
    boolean isAfter(long currentTime);

    /**
     * See {@link #isAfter(long, EpochTimeWindow)}
     * 
     * @param currentTime
     * @return boolean
     */
    boolean isBefore(long currentTime);

    /**
     * Check if the time window is empty.
     * 
     * @return boolean
     */
    boolean isEmpty();

    /**
     * See {@link #isWithin(long, EpochTimeWindow)}
     * 
     * @param currentTime
     * @return boolean
     */
    boolean isWithin(long currentTime);

    /**
     * Compare two EpochTimeWindow.
     * 
     * Compare two empty EpcohTimeWindow will results in 0. Empty
     * EpochTimeWindows is always less than non-Empty one.
     * 
     * With same beginTime, shorter duration window is always less than larger
     * duration window.
     * 
     * @param left
     * @param right
     * @return
     */
    static int compare(EpochTimeWindow left, EpochTimeWindow right) {
        long leftValue = left.isEmpty() ? Long.MIN_VALUE : left.getBeginTime();
        long rightValue = right.isEmpty() ? Long.MIN_VALUE : right.getBeginTime();

        int result = Long.compare(leftValue, rightValue);
        if (0 != result) {
            return result;
        }

        leftValue = left.isEmpty() ? Long.MIN_VALUE : left.getEndTime();
        rightValue = right.isEmpty() ? Long.MIN_VALUE : right.getEndTime();

        result = Long.compare(leftValue, rightValue);
        return result;
    }
}
