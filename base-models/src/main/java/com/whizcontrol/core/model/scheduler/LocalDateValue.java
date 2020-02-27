/**
 * 
 */
package com.whizcontrol.core.model.scheduler;

import java.time.DateTimeException;
import java.time.LocalDate;

import com.whizcontrol.core.model.json.BaseJsonModel;
import com.whizcontrol.datastore.exceptions.DsDataValidationException;

/**
 * LocalDate is immutable. This is used for JSON serialization.
 * 
 * @author yongli
 *
 */
public class LocalDateValue extends BaseJsonModel implements ScheduleValue<LocalDate> {
    private static final long serialVersionUID = 2048818126046133970L;

    private int year;
    private int month;
    private int day;
    private transient LocalDate value;

    /**
     * Constructor.
     * 
     * @param value
     */
    public LocalDateValue(final LocalDate value) {
        this.value = value;
        if (null != value) {
            this.year = this.value.getYear();
            this.month = this.value.getMonthValue();
            this.day = this.value.getDayOfMonth();
        }
    }

    protected LocalDateValue() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LocalDateValue)) {
            return false;
        }
        LocalDateValue other = (LocalDateValue) obj;
        if (this.day != other.day) {
            return false;
        }
        if (this.month != other.month) {
            return false;
        }
        if (this.year != other.year) {
            return false;
        }
        return true;
    }

    public int getDay() {
        validateValue();
        return this.day;
    }

    public int getMonth() {
        validateValue();
        return this.month;
    }

    @Override
    public LocalDate getValue() {
        validateValue();
        return this.value;
    }

    public int getYear() {
        validateValue();
        return this.year;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.day;
        result = prime * result + this.month;
        result = prime * result + this.year;
        return result;
    }

    public void setDay(int day) {
        this.day = day;
        value = null;
    }

    public void setMonth(int month) {
        this.month = month;
        this.value = null;
    }

    public void setYear(int year) {
        this.year = year;
        this.value = null;
    }

    @Override
    public void validateValue() {
        if (null != value) {
            return;
        }
        try {
            value = LocalDate.of(year, month, day);
        } catch (DateTimeException exp) {
            throw new DsDataValidationException(exp.getLocalizedMessage());
        }
    }

    @Override
    public LocalDateValue clone() {
        // value is immutable
        return (LocalDateValue) super.clone();
    }
}
