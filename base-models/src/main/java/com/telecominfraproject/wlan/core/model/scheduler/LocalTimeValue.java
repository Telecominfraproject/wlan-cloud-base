/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.DateTimeException;
import java.time.LocalTime;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;

/**
 * LocalTime is immutable. This is used for JSON serialization. Only support
 * hour and minute.
 * 
 * @author yongli
 *
 */
public class LocalTimeValue extends BaseJsonModel implements ScheduleValue<LocalTime> {
    private static final long serialVersionUID = -2243429909509074607L;

    /**
     * Hour [0-23]
     */
    private int hour;
    /**
     * Minute [0:59]
     */
    private int minute;

    private transient LocalTime value;

    /**
     * Constructor, will only copy hour and minute from value.
     * 
     * @param value
     */
    public LocalTimeValue(final LocalTime value) {
        this.value = value;
        if (null != value) {
            this.hour = this.value.getHour();
            this.minute = this.value.getMinute();
        }
    }

    protected LocalTimeValue() {
    }

    @Override
    public LocalTimeValue clone() {
        return (LocalTimeValue) super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LocalTimeValue)) {
            return false;
        }
        LocalTimeValue other = (LocalTimeValue) obj;
        if (this.hour != other.hour) {
            return false;
        }
        if (this.minute != other.minute) {
            return false;
        }
        return true;
    }

    public int getHour() {
        return this.hour;
    }

    public int getMinute() {
        return this.minute;
    }

    @Override
    public LocalTime getValue() {
        validateValue();
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.hour;
        result = prime * result + this.minute;
        return result;
    }

    public void setHour(int hour) {
        this.hour = hour;
        this.value = null;
    }

    public void setMinute(int minute) {
        this.minute = minute;
        this.value = null;
    }
    
    /**
     * Validate the value.
     * 
     * @throws DsDataValidationException
     */
    @Override
    public void validateValue() {
        if (null != value) {
            return;
        }
        try {
            this.value = LocalTime.of(this.hour, this.minute);
        } catch (DateTimeException exp) {
            throw new DsDataValidationException(exp.getLocalizedMessage());
        }
    }
}
