/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.LocalDateTime;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;

/**
 * @author yongli
 *
 */
public class LocalDateTimeValue extends BaseJsonModel implements ScheduleValue<LocalDateTime> {
    private static final long serialVersionUID = -595276875144024148L;

    /**
     * Local date
     */
    private LocalDateValue date;

    /**
     * Local time
     */
    private LocalTimeValue time;

    /**
     * cached date time
     */
    private transient LocalDateTime value;

    /**
     * Constructor, will only copy hour and minute from value.
     * 
     * @param value
     */
    public LocalDateTimeValue(final LocalDateTime value) {
        if (null != value) {
            this.setDate(new LocalDateValue(value.toLocalDate()));
            this.setTime(new LocalTimeValue(value.toLocalTime()));
        }
    }

    protected LocalDateTimeValue() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LocalDateTimeValue)) {
            return false;
        }
        LocalDateTimeValue other = (LocalDateTimeValue) obj;
        if (this.date == null) {
            if (other.date != null) {
                return false;
            }
        } else if (!this.date.equals(other.date)) {
            return false;
        }
        if (this.time == null) {
            if (other.time != null) {
                return false;
            }
        } else if (!this.time.equals(other.time)) {
            return false;
        }
        return true;
    }

    public LocalDateValue getDate() {
        return date;
    }

    public LocalTimeValue getTime() {
        return time;
    }

    @Override
    public LocalDateTime getValue() {
        validateValue();
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.date == null) ? 0 : this.date.hashCode());
        result = prime * result + ((this.time == null) ? 0 : this.time.hashCode());
        return result;
    }

    public void setDate(LocalDateValue date) {
        this.date = date;
        this.value = null;
    }

    public void setTime(LocalTimeValue time) {
        this.time = time;
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
        if (null == this.date) {
            throw new DsDataValidationException("Missing date");
        }
        if (null == this.time) {
            throw new DsDataValidationException("Missing time");
        }
        this.date.validateValue();
        this.time.validateValue();
        this.value = this.time.getValue().atDate(this.date.getValue());
    }
}
