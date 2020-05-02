/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Days of Week, with time range.
 * 
 * Default week days is every single week day
 * 
 * @author yongli
 *
 */
public class DaysOfWeekTimeRangeSchedule extends LocalTimeRangeSchedule {
    private static final long serialVersionUID = -3361176176172648354L;
    private static final Logger LOG = LoggerFactory.getLogger(DaysOfWeekTimeRangeSchedule.class);

    /**
     * Default days of week is every single week day
     */
    private static final SortedSet<DayOfWeek> DEFAULT_DAYS_OF_WEEK = new TreeSet<>(
            Arrays.asList(DayOfWeek.values()));

    private SortedSet<DayOfWeek> daysOfWeek;

    public DaysOfWeekTimeRangeSchedule(String timezone, SortedSet<DayOfWeek> daysOfWeek, LocalTime timeBegin,
            LocalTime timeEnd) {
        super(timezone, timeBegin, timeEnd);
        this.setDaysOfWeek(daysOfWeek);
    }

    protected DaysOfWeekTimeRangeSchedule() {
    }

    /**
     * Used for testing only.
     * 
     * @param testDuration
     */
    protected DaysOfWeekTimeRangeSchedule(Duration testDuration) {
        super(null, testDuration);
    }

    public SortedSet<DayOfWeek> getDaysOfWeek() {
        if ((null == daysOfWeek) || daysOfWeek.isEmpty()) {
            return DEFAULT_DAYS_OF_WEEK;
        }
        return daysOfWeek;
    }

    public void setDaysOfWeek(SortedSet<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected void populateUpcomingDays(final ValidDaysCollector collector) {
        collector.setCurrentDateIsValid(daysOfWeek.contains(collector.getCurrentDayOfWeek()));
        // start from current day, just to the next week day in the list
        for (LocalDate current = collector.getFirstCandidate(); !collector.isCompleted(current); current = current
                .with(TemporalAdjusters.next(this.daysOfWeek.first()))) {
            // get the desired week days in the list from the current day
            SortedSet<DayOfWeek> restDays = daysOfWeek.tailSet(current.getDayOfWeek());
            for (DayOfWeek dayOfWeek : restDays) {
                LocalDate dayToAdd;
                if (dayOfWeek.equals(current.getDayOfWeek())) {
                    dayToAdd = current;
                } else {
                    // advance to the upcoming day of week
                    dayToAdd = current.with(TemporalAdjusters.next(dayOfWeek));
                }
                if ((null != dayToAdd) && (ValidDaysCollector.testThenAdd(collector, dayToAdd))) {
                    break;
                }
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(daysOfWeek);
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
        if (!(obj instanceof DaysOfWeekTimeRangeSchedule)) {
            return false;
        }
        DaysOfWeekTimeRangeSchedule other = (DaysOfWeekTimeRangeSchedule) obj;
        return Objects.equals(daysOfWeek, other.daysOfWeek);
    }

    public static void main(String[] args) {
    	SortedSet<DayOfWeek> dow = new TreeSet<DayOfWeek>();
    	dow.add(DayOfWeek.SUNDAY);
    	dow.add(DayOfWeek.WEDNESDAY);
		LocalTime tBegin = LocalTime.of(22, 00);
		LocalTime tEnd = LocalTime.of(23, 30);
		DaysOfWeekTimeRangeSchedule ds = new DaysOfWeekTimeRangeSchedule(TimeZone.getTimeZone(ZoneId.systemDefault()).getID(), dow , tBegin, tEnd);
		System.out.println(ds);
	}
}
