/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * To use the filter, caller must initialize the collector by calling
 * {@link #setCurrentDateIsValid(boolean)}.
 * <p>
 * Then start search from {@link #getFirstCandidate()} until
 * {@link #isCompleted(LocalDate)} returns true.
 * <p>
 * Add each valid day use {@link #addValidDays(LocalDate)}
 * 
 * @author yongli
 */
public class ValidDaysCollector {
    /**
     * Only add date if collector {@link #isCompleted(LocalDate)} return false.
     * 
     * @param collector
     * @param withDayOfMonth
     * @return true if collector {@link #isCompleted(LocalDate)} returns true
     */
    public static boolean testThenAdd(final ValidDaysCollector collector, LocalDate date) {
        if (collector.isCompleted(date)) {
            return true;
        }
        collector.addValidDays(date);
        return false;
    }

    /**
     * Current date
     */
    private final LocalDateTime currentDateTime;
    private Boolean currentDateIsValid;
    private final SortedSet<LocalDate> validDays = new TreeSet<>();
    private final boolean normalTimeRange;

    private LocalDate lastExpectedDate;

    public ValidDaysCollector(final LocalDateTime currentDateTime, boolean normalTimeRange) {
        this.currentDateTime = currentDateTime;
        this.normalTimeRange = normalTimeRange;
    }

    /**
     * Add a valid date. Day must be inserted in order.
     * 
     * @param date
     */
    public void addValidDays(final LocalDate date) {
        if (skipDate(date)) {
            return;
        }
        // add this
        this.validDays.add(date);
        if (null == this.lastExpectedDate) {
            LocalDate lastDate = this.validDays.last();
            LocalDate currentDate = getCurrentDate();
            if (lastDate.isAfter(currentDate)) {
                if (this.normalTimeRange) {
                    // normal time range (0-6) only need one day past current
                    // date
                    this.lastExpectedDate = lastDate;
                } else {
                    // reverse time range (22-6): we need the two consecutive
                    // days since current date
                    if (lastDate.equals(currentDate.minusDays(-1))) {
                        // last found is the next day
                        this.lastExpectedDate = lastDate;
                    } else {
                        // last found is beyond next day
                        this.lastExpectedDate = lastDate.minusDays(-1);
                    }
                }
            }
        }
    }

    public LocalDate getCurrentDate() {
        return currentDateTime.toLocalDate();
    }

    /**
     * Get the day of month for the current date
     * 
     * @return day of month
     */
    public int getCurrentDayOfMonth() {
        return getCurrentDate().getDayOfMonth();
    }

    /**
     * Get the day of week for the current date
     * 
     * @return day of week
     */
    public DayOfWeek getCurrentDayOfWeek() {
        return getCurrentDate().getDayOfWeek();
    }

    public LocalDateTime getCurrentTime() {
        return currentDateTime;
    }

    /**
     * Get the first candidate. Must initialize the collector by calling
     * {@link #setCurrentDateIsValid(boolean)}.
     * 
     * @return
     */
    public LocalDate getFirstCandidate() {
        LocalDate currentDate = getCurrentDate();
        if (null == currentDateIsValid) {
            throw new IllegalStateException("Collector is not initialized");
        }
        if (!normalTimeRange && currentDateIsValid) {
            // reverse time range and today is valid, start with the
            // yesterday to test for begin time from yesterday
            return currentDate.minusDays(1);
        }
        // search from tomorrow because today is already processed
        return currentDate.minusDays(-1);
    }

    public SortedSet<LocalDate> getValidDays() {
        return validDays;
    }

    /**
     * Test if we found any validate day
     * 
     * @return
     */
    public boolean hasValidDays() {
        return !validDays.isEmpty();
    }

    /**
     * Test if search is completed
     * 
     * @param nextDate
     * @return
     */
    public boolean isCompleted(final LocalDate nextDate) {
        if (null != this.lastExpectedDate) {
            return nextDate.isAfter(this.lastExpectedDate);
        }
        return false;
    }

    /**
     * Set current date as a valid day. It will be inserted.
     * 
     * @param isValid
     */
    public void setCurrentDateIsValid(boolean isValid) {
        this.currentDateIsValid = isValid;
        if (isValid) {
            this.validDays.add(getCurrentDate());
        }
    }

    /**
     * Test if the date should be skipped
     * 
     * @param date
     * @return
     */
    public boolean skipDate(LocalDate date) {
        return this.validDays.contains(date);
    }
}
