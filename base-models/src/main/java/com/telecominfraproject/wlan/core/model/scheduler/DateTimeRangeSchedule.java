/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;

/**
 * @author yongli
 *
 */
public class DateTimeRangeSchedule extends ScheduleSetting {

    private static final long serialVersionUID = 3733060362061446842L;

    private static final Logger LOG = LoggerFactory.getLogger(DateTimeRangeSchedule.class);

    private LocalDateTimeValue timeBegin;
    private LocalDateTimeValue timeEnd;

    public DateTimeRangeSchedule(String timezone, final LocalDateTime timeBegin, final LocalDateTime timeEnd) {
        super(timezone);
        if (null != timeBegin) {
            this.timeBegin = new LocalDateTimeValue(timeBegin);
        }
        if (null != timeEnd) {
            this.timeEnd = new LocalDateTimeValue(timeEnd);
        }
    }

    protected DateTimeRangeSchedule() {
        super(null);
    }

    /**
     * Use for testing only
     * 
     * @param testDuration
     */
    protected DateTimeRangeSchedule(Duration testDuration) {
        super(null);
        LocalDateTime now = LocalDateTime.now();
        this.timeBegin = new LocalDateTimeValue(now);
        this.timeEnd = new LocalDateTimeValue(now.plus(testDuration));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.telecominfraproject.wlan.scheduler.models.ScheduleSetting#getNextWindows(long)
     */
    @Override
    public EpochTimeWindow getNextWindows(long currentTime) {
        validateSetting(null);
        ZonedDateTime currentDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), getLocalZoneId())
                .atZone(getLocalZoneId());
        ZonedDateTime beginDateTime = null;
        ZonedDateTime endDateTime = null;

        ZonedDateTime settingBeginTime = this.timeBegin.getValue().atZone(getLocalZoneId());
        LocalDateTime timeEndValue = this.timeEnd.getValue();
        // end time needs to cover the whole minute
        ZonedDateTime settingEndTime = LocalDateTime.of(timeEndValue.toLocalDate(), LocalTime.of(timeEndValue.getHour(),
                timeEndValue.getMinute(), LocalTime.MAX.getSecond(), LocalTime.MAX.getNano())).atZone(getLocalZoneId());
        if (currentDateTime.isBefore(settingEndTime)) {
            beginDateTime = settingBeginTime;
            endDateTime = settingEndTime;
        }

        if ((null != beginDateTime) && (null != endDateTime)) {
            getLogger().debug("getNextWindows(): found next window from {} for {}: [{},{}]", currentDateTime, this,
                    beginDateTime, endDateTime);
            return new ImmutableTimeWindow(beginDateTime.toInstant().toEpochMilli(), endDateTime.toInstant().toEpochMilli());
        }
        // no warning if we can't find a window because it's a fixed range
        getLogger().debug("getNextWindows(): No window found from {} for {}: [{},{}]", currentDateTime, this,
                beginDateTime, endDateTime);
        return null;
    }

    public LocalDateTimeValue getTimeBegin() {
        return timeBegin;
    }

    public LocalDateTimeValue getTimeEnd() {
        return timeEnd;
    }

    public void setTimeBegin(LocalDateTimeValue timeBegin) {
        this.timeBegin = timeBegin;
    }

    public void setTimeEnd(LocalDateTimeValue timeEnd) {
        this.timeEnd = timeEnd;
    }

    @Override
    public void validateSetting(Duration minimumDuration) {
        super.validateSetting(minimumDuration);
        if (null == this.timeBegin) {
            throw new DsDataValidationException("Missing timeBegin");
        }
        this.timeBegin.validateValue();
        if (null == this.timeEnd) {
            throw new DsDataValidationException("Missing timeEnd");
        }
        this.timeEnd.validateValue();
        if (!this.timeBegin.getValue().isBefore(this.timeEnd.getValue())) {
            throw new DsDataValidationException("timeBegin must be less than timeEnd");
        }
        if (null != minimumDuration) {
            Duration currentDuraion = Duration.between(this.timeBegin.getValue().atZone(getLocalZoneId()),
                    this.timeEnd.getValue().atZone(getLocalZoneId()));
            if (currentDuraion.compareTo(minimumDuration) < 0) {
                throw new DsDataValidationException(
                        "Not enough time," + " minimum required " + minimumDuration + ": " + currentDuraion);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.telecominfraproject.wlan.scheduler.models.ScheduleSetting#getLogger()
     */
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(timeBegin, timeEnd);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DateTimeRangeSchedule)) {
            return false;
        }
        DateTimeRangeSchedule other = (DateTimeRangeSchedule) obj;
        return Objects.equals(timeBegin, other.timeBegin) && Objects.equals(timeEnd, other.timeEnd);
    }

}
