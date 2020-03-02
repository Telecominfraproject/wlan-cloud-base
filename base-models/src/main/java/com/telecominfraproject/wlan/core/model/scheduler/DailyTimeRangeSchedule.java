/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daily time range.
 * 
 * @author yongli
 *
 */
public class DailyTimeRangeSchedule extends LocalTimeRangeSchedule {
    private static final long serialVersionUID = 8702501314503264670L;
    private static final Logger LOG = LoggerFactory.getLogger(DailyTimeRangeSchedule.class);

    public DailyTimeRangeSchedule(final String timezone, final LocalTime timeBegin, final LocalTime timeEnd) {
        super(timezone, timeBegin, timeEnd);
    }

    protected DailyTimeRangeSchedule() {
        super();
    }

    /**
     * Use for testing only
     * 
     * @param testDuration
     */
    DailyTimeRangeSchedule(Duration testDuration) {
        super(null, testDuration);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected void populateUpcomingDays(final ValidDaysCollector collector) {
        collector.setCurrentDateIsValid(true);
        for (LocalDate current = collector.getFirstCandidate(); !collector.isCompleted(current); current = current
                .minusDays(-1)) {
            if (!collector.skipDate(current)) {
                collector.addValidDays(current);
            }
        }
    }
}
