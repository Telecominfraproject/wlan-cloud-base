/**
 * 
 */
package com.whizcontrol.core.model.scheduler;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.SortedSet;

import com.whizcontrol.datastore.exceptions.DsDataValidationException;

/**
 * Time range with {@linkplain #timeBegin} and {@linkplain #timeEnd}
 * <p>
 * Default {@linkplain #timeBegin} is {@link LocalTime#MIN} and default
 * {@linkplain #timeEnd} is {@link LocalTime#MAX}.
 * <p>
 * Support timeBegin after timeEnd. Which results in two time range consists of
 * timeBegin to {@linkplain LocalTime#MAX} and {@linkplain LocalTime#MIDNIGHT}
 * to timeEnd.
 * 
 * For example. 21:00-04:00 will include time 00:00-04:00 and 21:00-23:59.
 * <p>
 * Use {@link #validateSetting(Duration)} to verify all values have correct
 * value.
 * 
 * @author yongli
 *
 */
public abstract class LocalTimeRangeSchedule extends ScheduleSetting {
    private static final long serialVersionUID = 3231831193188637229L;

    /**
     * beginning of the time range, default {@link LocalTime#MIN}
     */
    private LocalTimeValue timeBegin;
    /**
     * ending of the time range, default {@link LocalTime#MAX}
     */
    private LocalTimeValue timeEnd;

    /**
     * Constructor for TimeRangeSetting
     * 
     * @param timezone
     *            - timezone. see {@link ZoneId#getAvailableZoneIds()}
     * @param timeBegin
     *            - beginning of the time range, default {@link LocalTime#MIN}
     * @param timeEnd
     *            - ending of the time range, default {@link LocalTime#MAX}
     */
    public LocalTimeRangeSchedule(final String timezone, final LocalTime timeBegin, final LocalTime timeEnd) {
        super(timezone);
        if (null == timeBegin) {
            this.timeBegin = new LocalTimeValue(LocalTime.MIN);
        } else {
            this.timeBegin = new LocalTimeValue(timeBegin);
        }
        if (null == timeEnd) {
            this.timeEnd = new LocalTimeValue(LocalTime.MAX);
        } else {
            this.timeEnd = new LocalTimeValue(timeEnd);
        }
    }

    protected LocalTimeRangeSchedule() {
        super(null);
    }

    protected LocalTimeRangeSchedule(final String timezone, Duration testDuration) {
        this(timezone, LocalTime.MIN, LocalTime.ofNanoOfDay(testDuration.toNanos()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LocalTimeRangeSchedule)) {
            return false;
        }
        LocalTimeRangeSchedule other = (LocalTimeRangeSchedule) obj;
        return Objects.equals(timeBegin, other.timeBegin) && Objects.equals(timeEnd, other.timeEnd);
    }

    @Override
    public EpochTimeWindow getNextWindows(long currentTime) {
        validateSetting(null);
        // begin time will start from the exact time
        LocalTime timeBeginValue = timeBegin.getValue();
        // end time will cover the entire minute
        LocalTime timeEndValue = LocalTime.of(timeEnd.getValue().getHour(), timeEnd.getValue().getMinute(),
                LocalTime.MAX.getSecond(), LocalTime.MAX.getNano());

        // keep the upcomingDays sorted
        LocalDateTime currentDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), getLocalZoneId());
        final ValidDaysCollector dateCollector = new ValidDaysCollector(currentDateTime,
                timeBeginValue.isBefore(timeEndValue));
        populateUpcomingDays(dateCollector);

        EpochTimeWindow result = null;
        if (!dateCollector.hasValidDays()) {
            getLogger().warn("getNextWindows(): No window found from {} for {}: empty upcoming days",
                    currentDateTime.atZone(getLocalZoneId()), this);
        } else {
            if (timeBeginValue.isBefore(timeEndValue)) {
                result = findNextWindowInNormalTimeRange(timeBeginValue, timeEndValue, dateCollector);
            } else {
                result = findNextWindowInReverseTimeRange(timeBeginValue, timeEndValue, dateCollector);
            }
        }
        return result;
    }

    public LocalTimeValue getTimeBegin() {
        return timeBegin;
    }

    public LocalTimeValue getTimeEnd() {
        return timeEnd;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(timeBegin, timeEnd);
        return result;
    }

    public void setTimeBegin(LocalTimeValue timeBegin) {
        this.timeBegin = timeBegin;
    }

    public void setTimeEnd(LocalTimeValue timeEnd) {
        this.timeEnd = timeEnd;
    }

    @Override
    public void validateSetting(Duration minimumDuration) {
        super.validateSetting(minimumDuration);
        try {
            if (null == this.timeBegin) {
                this.timeBegin = new LocalTimeValue(LocalTime.MIN);
            }
            if (null == this.timeEnd) {
                this.timeEnd = new LocalTimeValue(LocalTime.MAX);
            }
            if (timeBegin.equals(timeEnd)) {
                throw new DsDataValidationException("same value specified for begin and end");
            }
            if (null != minimumDuration) {
                Duration currentDuraion = Duration.between(timeBegin.getValue(), timeEnd.getValue());
                if (currentDuraion.isNegative()) {
                    // reverse windows
                    currentDuraion = currentDuraion.minusDays(-1);
                }
                if (currentDuraion.compareTo(minimumDuration) < 0) {
                    throw new DsDataValidationException(
                            "Not enough time," + " minimum required " + minimumDuration + ": " + currentDuraion);
                }
            }
        } catch (DateTimeException exp) {
            throw new DsDataValidationException(exp.getLocalizedMessage());
        }
    }

    /**
     * Find the next window for a time range with timeBeginValue < timeEndValue.
     * E.g. 18:00-23:00
     * 
     * @param timeBeginValue
     * @param timeEndValue
     * @param collector
     * @return
     */
    private EpochTimeWindow findNextWindowInNormalTimeRange(LocalTime timeBeginValue, LocalTime timeEndValue,
            final ValidDaysCollector collector) {
        LocalDateTime currentDateTime = collector.getCurrentTime();
        SortedSet<LocalDate> validDays = collector.getValidDays();
        ZonedDateTime beginDateTime = null;
        ZonedDateTime endDateTime = null;
        for (LocalDate day : validDays) {
            // skip the day if end time already passed
            if (timeEndValue.atDate(day).isBefore(currentDateTime)) {
                continue;
            }
            // save the end time
            beginDateTime = timeBeginValue.atDate(day).atZone(getLocalZoneId());
            endDateTime = timeEndValue.atDate(day).atZone(getLocalZoneId());
            break;
        }
        if ((null != beginDateTime) && (null != endDateTime)) {
            getLogger().debug("getNextWindows(): found next window from {} for {}: [{},{}]",
                    currentDateTime.atZone(getLocalZoneId()), this, beginDateTime, endDateTime);
            return new ImmutableTimeWindow(beginDateTime.toInstant().toEpochMilli(),
                    endDateTime.toInstant().toEpochMilli());
        }
        getLogger().warn("getNextWindows(): No window found from {} for {}: [{},{}]",
                currentDateTime.atZone(getLocalZoneId()), this, beginDateTime, endDateTime);
        return null;
    }

    /**
     * Find the next window for a time range with timeBeginValue > timeEndValue.
     * E.g. 23:00-04:00
     * 
     * @param timeBeginValue
     * @param timeEndValue
     * @param collector
     * @return
     */
    private EpochTimeWindow findNextWindowInReverseTimeRange(LocalTime timeBeginValue, LocalTime timeEndValue,
            final ValidDaysCollector collector) {
        LocalDateTime currentDateTime = collector.getCurrentTime();
        SortedSet<LocalDate> validDays = collector.getValidDays();
        ZonedDateTime beginDateTime = null;
        ZonedDateTime endDateTime = null;
        LocalDate today = currentDateTime.toLocalDate();
        // look for first end day past current time
        for (LocalDate day : validDays) {
            if (timeEndValue.atDate(day).isAfter(currentDateTime)) {
                // stop the moment we found one end time is greater than now
                endDateTime = timeEndValue.atDate(day).atZone(getLocalZoneId());
                LocalDate dayBefore = day.minusDays(1);
                if (validDays.contains(dayBefore)) {
                    // start at the begin time of the day before
                    beginDateTime = timeBeginValue.atDate(dayBefore).atZone(getLocalZoneId());
                } else {
                    // start the the beginning of the day
                    beginDateTime = LocalTime.MIN.atDate(day).atZone(getLocalZoneId());
                }
                break;
            }

            // check the begin time
            if (!day.isBefore(today)) {
                LocalDateTime timeBeginAtDay = timeBeginValue.atDate(day);
                if (currentDateTime.isAfter(timeBeginAtDay)) {
                    beginDateTime = timeBeginAtDay.atZone(getLocalZoneId());
                    LocalDate nextDay = day.minusDays(-1);
                    if (validDays.contains(nextDay)) {
                        // stop at the end time next day
                        endDateTime = timeEndValue.atDate(nextDay).atZone(getLocalZoneId());
                    } else {
                        // stop at the end of current day
                        endDateTime = LocalTime.MAX.atDate(day).atZone(getLocalZoneId());
                    }
                    break;
                }
            }
        }
        if ((null != beginDateTime) && (null != endDateTime)) {
            getLogger().debug("getNextWindows(): found next window from {} for {}: [{},{}]",
                    currentDateTime.atZone(getLocalZoneId()), this, beginDateTime, endDateTime);
            return new ImmutableTimeWindow(beginDateTime.toInstant().toEpochMilli(),
                    endDateTime.toInstant().toEpochMilli());
        }
        getLogger().warn("getNextWindows(): No window found from {} for {}: [{},{}]",
                currentDateTime.atZone(getLocalZoneId()), this, beginDateTime, endDateTime);
        return null;
    }

    /**
     * Search for a set of valid days.
     * 
     * See {@link ValidDaysCollector} on how to use the collector.
     * 
     * @param collector
     *            - date collector
     */
    abstract protected void populateUpcomingDays(final ValidDaysCollector collector);
}
