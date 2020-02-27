package com.whizcontrol.core.model.scheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Days of Month with Time Range Setting.
 * 
 * When the {@link #daysOfMonth} contains day of month beyond the maximum day in
 * the current month, the last day of the month will be used.
 * 
 * e.g. dayOfMonth = [1,31]. For 2017 February, the schedule will run on
 * February 1, 2017 and February 28, 2017.
 * 
 * @author yongli
 *
 */
public class DaysOfMonthTimeRangeSchedule extends LocalTimeRangeSchedule {

    private static final long serialVersionUID = -8086722479422175156L;
    private static final Logger LOG = LoggerFactory.getLogger(DaysOfMonthTimeRangeSchedule.class);
    private SortedSet<Integer> daysOfMonth;

    /**
     * Constructor. If daysOfMonth is null or empty, it will results in a full
     * set of days of month.
     * 
     * @param timezone
     * @param daysOfMonth
     * @param timeBegin
     * @param timeEnd
     */
    public DaysOfMonthTimeRangeSchedule(String timezone, SortedSet<Integer> daysOfMonth, LocalTime timeBegin,
            LocalTime timeEnd) {
        super(timezone, timeBegin, timeEnd);
        this.daysOfMonth = daysOfMonth;
    }

    protected DaysOfMonthTimeRangeSchedule() {
        super(null, null, null);
        this.daysOfMonth = null;
    }

    /**
     * Use for testing only
     * 
     * @param testDuration
     */
    protected DaysOfMonthTimeRangeSchedule(Duration testDuration) {
        super(null, testDuration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DaysOfMonthTimeRangeSchedule)) {
            return false;
        }
        DaysOfMonthTimeRangeSchedule other = (DaysOfMonthTimeRangeSchedule) obj;
        if (this.daysOfMonth == null) {
            if (other.daysOfMonth != null) {
                return false;
            }
        } else if (!this.daysOfMonth.equals(other.daysOfMonth)) {
            return false;
        }
        return true;
    }

    public SortedSet<Integer> getDaysOfMonth() {
        return daysOfMonth;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.daysOfMonth == null) ? 0 : this.daysOfMonth.hashCode());
        return result;
    }

    public void setDaysOfMonth(SortedSet<Integer> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    @Override
    public void validateSetting(Duration minimumDuration) {
        super.validateSetting(minimumDuration);
        if (null == this.daysOfMonth || this.daysOfMonth.isEmpty()) {
            this.daysOfMonth = new TreeSet<>();
            for (Long i = ChronoField.DAY_OF_MONTH.range().getMinimum(); i <= ChronoField.DAY_OF_MONTH.range()
                    .getMaximum(); ++i) {
                this.daysOfMonth.add(i.intValue());
            }
        } else {
            for (Integer dayOfMonth : this.daysOfMonth) {
                ChronoField.DAY_OF_MONTH.checkValidValue(dayOfMonth);
            }
        }
    }

    /**
     * Test if the current date in collector is a valid date. Need to support
     * last day of month.
     * 
     * @param collector
     * @return
     */
    private boolean isCurrentDateValid(ValidDaysCollector collector) {
        LocalDate currentDate = collector.getCurrentDate();
        if (this.daysOfMonth.contains(currentDate.getDayOfMonth())) {
            return true;
        }
        if (currentDate.equals(currentDate.with(TemporalAdjusters.lastDayOfMonth()))) {
            return (this.daysOfMonth.last() > currentDate.getDayOfMonth());
        }
        return false;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected void populateUpcomingDays(final ValidDaysCollector collector) {
        // first check the current date
        collector.setCurrentDateIsValid(isCurrentDateValid(collector));
        // Month loop
        for (LocalDate current = collector.getFirstCandidate(); !collector.isCompleted(current);
                // skip to first day of next month
                current = current.with(TemporalAdjusters.firstDayOfNextMonth())) {

            SortedSet<Integer> restDays = daysOfMonth.tailSet(current.getDayOfMonth());
            LocalDate lastDayOfMonth = null;

            // day of month loop
            for (Integer dayOfMonth : restDays) {
                if (dayOfMonth.equals(current.getDayOfMonth())) {
                    // current date is in the set
                    if (ValidDaysCollector.testThenAdd(collector, current)) {
                        break;
                    }
                    continue;
                }
                // make sure we don't beyond more than the last day of the
                // month
                if (null == lastDayOfMonth) {
                    lastDayOfMonth = current.with(TemporalAdjusters.lastDayOfMonth());
                }
                if (dayOfMonth >= lastDayOfMonth.getDayOfMonth()) {
                    // add last day of the month and done with the current
                    // month
                    ValidDaysCollector.testThenAdd(collector, lastDayOfMonth);
                    break;
                }
                // add the day of month
                if (ValidDaysCollector.testThenAdd(collector, current.withDayOfMonth(dayOfMonth))) {
                    break;
                }
            }
        }
    }

}
