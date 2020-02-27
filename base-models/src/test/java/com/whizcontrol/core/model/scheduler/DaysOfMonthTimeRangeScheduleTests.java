package com.whizcontrol.core.model.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.TreeSet;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whizcontrol.core.model.json.BaseJsonModel;
import com.whizcontrol.core.model.scheduler.DaysOfMonthTimeRangeSchedule;
import com.whizcontrol.core.model.scheduler.ScheduleSetting;
import com.whizcontrol.core.model.scheduler.EpochTimeWindow;
import com.whizcontrol.datastore.exceptions.DsDataValidationException;

public class DaysOfMonthTimeRangeScheduleTests {
    private static final String TIMEZONE = ZoneId.systemDefault().getId();
    private static final Logger LOG = LoggerFactory.getLogger(DaysOfMonthTimeRangeScheduleTests.class);

    @Test
    public void testJSONSerialization() {
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(1, 31)), LocalTime.of(18, 0), LocalTime.of(9, 0));
        String jsonString = setting.toPrettyString();
        LOG.debug("Testing {}", jsonString);
        ScheduleSetting result = BaseJsonModel.fromString(jsonString, ScheduleSetting.class);
        assertEquals(setting, result);
        assertFalse(setting.isEmpty());
    }

    @Test
    public void testNormalWindowBefore() {
        // [1,2,29] 9:00 am-12:00 pm
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(1, 2, 29)), LocalTime.of(9, 0), LocalTime.of(12, 0));

        // test before 1st day of month, 2017-10-31T11:00 (Sunday)
        ZonedDateTime now = LocalDateTime.of(2017, 10, 31, 11, 0).atZone(setting.getLocalZoneId());

        // expect 2017-11-01T09:00-2017-11-01T12:00 (Monday)
        long expectedBegin = LocalDateTime.of(2017, 11, 1, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 1, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testNormalWindowAfter() {
        // [1,2,29] 9:00 am-12:00 pm
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(1, 2, 29)), LocalTime.of(9, 0), LocalTime.of(12, 0));

        // test after 1st day schedule, 2017-11-01T013:00 (Sunday)
        ZonedDateTime now = LocalDateTime.of(2017, 11, 1, 13, 0).atZone(setting.getLocalZoneId());

        // expect 2nd day schedule
        long expectedBegin = LocalDateTime.of(2017, 11, 2, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 2, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testNormalWindowWithin() {
        // 9:00 am-12:00 pm
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(1, 31)), LocalTime.of(9, 0), LocalTime.of(12, 0));

        // test within last day of month, 2017-2-28T10:00 (Monday)
        ZonedDateTime now = LocalDateTime.of(2017, 2, 28, 10, 0).atZone(setting.getLocalZoneId());

        // expect last day of month schedule
        // 2017/2/28 9:00:00 - 2017/2/28 12:00:59
        long expectedBegin = LocalDateTime.of(2017, 2, 28, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 2, 28, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testDataValidation() {
        Duration duration = Duration.ofHours(1);

        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule("badTZ", null, LocalTime.of(0, 0),
                LocalTime.of(2, 0));
        checkInvalidSetting(setting, "invalid TZ", duration);

        setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE, null, null, LocalTime.of(0, 59));
        checkInvalidSetting(setting, "minimum duration", duration);

        setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE, null, LocalTime.of(23, 40), LocalTime.of(0, 19));
        checkInvalidSetting(setting, "minimum duration", duration);

        // verify normal setting
        setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE, null, null, LocalTime.of(1, 0));
        setting.validateSetting(duration);
        assertEquals(31, setting.getDaysOfMonth().size());

        // verify revert setting
        setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE, null, LocalTime.of(23, 0), LocalTime.MIDNIGHT);
        setting.validateSetting(duration);
        assertEquals(31, setting.getDaysOfMonth().size());
    }

    @Test
    public void testReverseWindowDayBefore1stSameMonth() {
        // 6:00 pm-9:00 am
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(2, 31)), LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test day before 1st window of any day: 2016-2-28T11:00
        ZonedDateTime now = LocalDateTime.of(2016, 2, 28, 11, 0).atZone(setting.getLocalZoneId());

        // schedule from 1st window of the following day of month
        // 00:00am-09:00am
        long expectedBegin = LocalDateTime.of(2016, 2, 29, 0, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2016, 2, 29, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowDayBefore1stNextMonth() {
        // 6:00 pm-9:00 am
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(2, 3)), LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test day before 1st window of any day: 2016-2-28T11:00
        ZonedDateTime now = LocalDateTime.of(2016, 2, 28, 11, 0).atZone(setting.getLocalZoneId());

        // schedule from 1st window of the following day of month
        // 00:00am-09:00am
        long expectedBegin = LocalDateTime.of(2016, 3, 2, 0, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2016, 3, 2, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowOutside() {
        // 6:00 pm -9:00 am
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(2, 3)), LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test between the two windows. i.e.between 9:00am - 6:00pm
        ZonedDateTime now = LocalDateTime.of(2017, 11, 2, 17, 0).atZone(setting.getLocalZoneId());

        // expect the 2nd window and 1st window of the next day
        long expectedBegin = LocalDateTime.of(2017, 11, 2, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 3, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin1st() {
        // 6:00 pm-9:00 am
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(2, 3)), LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test within 1st window, i.e. MONDAY between 0:00am-9:00am
        ZonedDateTime now = LocalDateTime.of(2017, 11, 2, 1, 0).atZone(setting.getLocalZoneId());

        // schedule of 1st window
        long expectedBegin = LocalDateTime.of(2017, 11, 2, 0, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 2, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin2ndNoGap() {
        // 6:00 pm-9:00 am
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(1, 31)), LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test within 2nd window, i.e. last day of month between 6:00pm-23:59pm
        ZonedDateTime now = LocalDateTime.of(2017, 2, 28, 19, 0).atZone(setting.getLocalZoneId());

        // schedule from 2nd window to end of 1st window next day
        long expectedBegin = LocalDateTime.of(2017, 2, 28, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 3, 1, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin2ndWithGap() {
        // 6:00 pm-9:00 am
        DaysOfMonthTimeRangeSchedule setting = new DaysOfMonthTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(2, 4)), LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test within 2nd window, i.e. 2nd day of month between 6:00pm -
        // 23:59pm
        ZonedDateTime now = LocalDateTime.of(2017, 11, 2, 19, 0).atZone(setting.getLocalZoneId());

        // schedule from now to end of day WEDNESDAY
        long expectedBegin = LocalDateTime.of(2017, 11, 2, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 3, 0, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals("beginTime", expectedBegin, result.getBeginTime());
        assertEquals("endTime", expectedEnd, result.getEndTime());
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
}
