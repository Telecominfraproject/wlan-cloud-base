package com.whizcontrol.core.model.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whizcontrol.core.model.json.BaseJsonModel;
import com.whizcontrol.datastore.exceptions.DsDataValidationException;

public class DateTimeRangeScheduleTests {

    private static final String TIMEZONE = ZoneId.systemDefault().getId();
    private static final Logger LOG = LoggerFactory.getLogger(DaysOfMonthTimeRangeScheduleTests.class);

    @Test
    public void testJSONSerialization() {
        DateTimeRangeSchedule setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 11, 12, 18, 0),
                LocalDateTime.of(2017, 11, 13, 4, 0));
        String jsonString = setting.toPrettyString();
        LOG.debug("Testing {}", jsonString);
        ScheduleSetting result = BaseJsonModel.fromString(jsonString, ScheduleSetting.class);
        assertEquals(setting, result);
        assertFalse(setting.isEmpty());
    }

    @Test
    public void testDataValidation() {
        Duration duration = Duration.ofHours(1);

        DateTimeRangeSchedule setting = new DateTimeRangeSchedule("badTZ", LocalDateTime.of(2017, 10, 1, 0, 0),
                LocalDateTime.of(2017, 10, 2, 0, 0));
        checkInvalidSetting(setting, "invalid TZ", duration);

        setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 10, 1, 0, 0),
                LocalDateTime.of(2017, 10, 1, 0, 59));
        checkInvalidSetting(setting, "minimum duration", duration);

        setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 10, 1, 0, 1),
                LocalDateTime.of(2017, 10, 1, 0, 0));
        checkInvalidSetting(setting, "negative duration", duration);

        // verify timeBegin
        setting = new DateTimeRangeSchedule(TIMEZONE, null, LocalDateTime.of(2017, 10, 1, 0, 0));
        checkInvalidSetting(setting, "missing timeBegin", duration);

        setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 10, 1, 0, 0), null);
        checkInvalidSetting(setting, "missing timeEnd", duration);

        // verify normal setting
        setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 10, 1, 0, 0),
                LocalDateTime.of(2017, 10, 1, 1, 0));
        setting.validateSetting(duration);
    }

    private void checkInvalidSetting(final ScheduleSetting setting, String check, Duration duration) {
        try {
            setting.validateSetting(duration);
            fail("failed " + check + ": " + setting);
        } catch (DsDataValidationException exp) {
            // pass
            LOG.debug("pass {}: {}", check, exp.getLocalizedMessage());
        }
    }

    @Test
    public void testBeforeWindow() {
        DateTimeRangeSchedule setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 3, 1, 0, 0),
                LocalDateTime.of(2017, 10, 1, 1, 0));
        // test 2017-2-28T00:00am which is before the schedule time
        ZonedDateTime currenDateTime = LocalDateTime.of(2017, 2, 28, 0, 0).atZone(setting.getLocalZoneId());

        ZonedDateTime expectedTimeBegin = setting.getTimeBegin().getValue().atZone(setting.getLocalZoneId());
        ZonedDateTime expectedTimeEnd = setting.getTimeEnd().getValue().atZone(setting.getLocalZoneId());

        EpochTimeWindow nextWindow = setting.getNextWindows(currenDateTime.toInstant().toEpochMilli());

        assertNotNull(nextWindow);
        assertEquals(expectedTimeBegin.toInstant().toEpochMilli(), nextWindow.getBeginTime());
        assertEquals(expectedTimeEnd.toInstant().toEpochMilli() + TimeUnit.MINUTES.toMillis(1) - 1,
                nextWindow.getEndTime());
    }

    @Test
    public void testWithinWindow() {
        DateTimeRangeSchedule setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 3, 1, 0, 0),
                LocalDateTime.of(2017, 10, 1, 1, 0));
        // test 2017-3-2T00:00am which is within the schedule time
        ZonedDateTime currenDateTime = LocalDateTime.of(2017, 3, 2, 0, 0).atZone(setting.getLocalZoneId());

        ZonedDateTime expectedTimeBegin = setting.getTimeBegin().getValue().atZone(setting.getLocalZoneId());
        ZonedDateTime expectedTimeEnd = setting.getTimeEnd().getValue().atZone(setting.getLocalZoneId());

        EpochTimeWindow nextWindow = setting.getNextWindows(currenDateTime.toInstant().toEpochMilli());

        assertNotNull(nextWindow);
        assertEquals(expectedTimeBegin.toInstant().toEpochMilli(), nextWindow.getBeginTime());
        assertEquals(expectedTimeEnd.toInstant().toEpochMilli() + TimeUnit.MINUTES.toMillis(1) - 1,
                nextWindow.getEndTime());
    }

    @Test
    public void testPastWindow() {
        DateTimeRangeSchedule setting = new DateTimeRangeSchedule(TIMEZONE, LocalDateTime.of(2017, 3, 1, 0, 0),
                LocalDateTime.of(2017, 10, 1, 1, 0));
        // test 2017-10-1T1:01am which is past the schedule time
        ZonedDateTime currenDateTime = LocalDateTime.of(2017, 10, 1, 1, 1).atZone(setting.getLocalZoneId());
        EpochTimeWindow nextWindow = setting.getNextWindows(currenDateTime.toInstant().toEpochMilli());
        assertNull(nextWindow);
    }

}
