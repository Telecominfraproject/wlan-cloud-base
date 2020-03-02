package com.telecominfraproject.wlan.core.model.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.DayOfWeek;
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

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.scheduler.DaysOfWeekTimeRangeSchedule;
import com.telecominfraproject.wlan.core.model.scheduler.EpochTimeWindow;
import com.telecominfraproject.wlan.core.model.scheduler.ScheduleSetting;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;

public class DaysOfWeekTimeRangeScheduleTests {
    private static final String TIMEZONE = ZoneId.systemDefault().getId();
    private static final Logger LOG = LoggerFactory.getLogger(DailyTimeRangeScheduleTests.class);

    @Test
    public void testDataValidation() {
        Duration duration = Duration.ofHours(1);

        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule("badTZ", null, LocalTime.of(0, 0),
                LocalTime.of(2, 0));
        checkInvalidSetting(setting, "invalid TZ", duration);

        setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE, null, null, LocalTime.of(0, 59));
        checkInvalidSetting(setting, "minimum duration", duration);

        setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE, null, LocalTime.of(23, 40), LocalTime.of(0, 19));
        checkInvalidSetting(setting, "minimum duration", duration);

        // verify normal setting
        setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE, null, null, LocalTime.of(1, 0));
        setting.validateSetting(duration);
        assertEquals(7, setting.getDaysOfWeek().size());

        // verify revert setting
        setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE, null, LocalTime.of(23, 0), LocalTime.MIDNIGHT);
        setting.validateSetting(duration);
        assertEquals(7, setting.getDaysOfWeek().size());
    }

    @Test
    public void testJSONSerialization() {
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)), LocalTime.of(18, 0),
                LocalTime.of(9, 0));
        String jsonString = setting.toPrettyString();
        LOG.debug("Testing {}", jsonString);
        ScheduleSetting result = BaseJsonModel.fromString(jsonString, ScheduleSetting.class);
        assertEquals(setting, result);
        assertFalse(setting.isEmpty());
    }

    @Test
    public void testNormalWindowAfter() {
        // 9:00 am-12:00 pm
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)), LocalTime.of(9, 0),
                LocalTime.of(12, 0));

        // test after 1st week day schedule, 2017-11-06T13:00 (Monday)
        ZonedDateTime now = LocalDateTime.of(2017, 11, 6, 13, 0).atZone(setting.getLocalZoneId());
        // expect the 2nd week day schedule, 2017-11-07T9:00-2017-11-07T12:00
        // (Tuesday)
        long expectedBegin = LocalDateTime.of(2017, 11, 7, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 7, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testNormalWindowBefore() {
        // 9:00 am-12:00 pm
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)), LocalTime.of(9, 0),
                LocalTime.of(12, 0));

        // test before 1st week day, 2017-11-05T01:00 (Sunday)
        ZonedDateTime now = LocalDateTime.of(2017, 11, 5, 11, 0).atZone(setting.getLocalZoneId());

        // expect 2017-11-06T09:00-2017-11-06T12:00 (Monday)
        long expectedBegin = LocalDateTime.of(2017, 11, 6, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 6, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());

        // test before 2nd week day
        now = LocalDateTime.of(2017, 11, 6, 13, 0).atZone(setting.getLocalZoneId());
        expectedBegin = LocalDateTime.of(2017, 11, 7, 9, 0).atZone(setting.getLocalZoneId()).toInstant().toEpochMilli();
        expectedEnd = LocalDateTime.of(2017, 11, 7, 12, 1).atZone(setting.getLocalZoneId()).toInstant().toEpochMilli()
                - 1;
        result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testNormalWindowWithin() {
        // 9:00 am-12:00 pm
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)), LocalTime.of(9, 0),
                LocalTime.of(12, 0));

        // test within 1st week day, 2017-11-06T10:00 (Monday)
        ZonedDateTime now = LocalDateTime.of(2017, 11, 6, 10, 0).atZone(setting.getLocalZoneId());

        // expect 2017-11-06T09:00-2017-11-06T12:00 (Monday)
        long expectedBegin = LocalDateTime.of(2017, 11, 6, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 6, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowDayBefore1st() {
        // 6:00 pm-9:00 am
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)), LocalTime.of(18, 0),
                LocalTime.of(9, 0));

        // test day before 1st window: 2017-11-5T11:00 (Sunday)
        ZonedDateTime now = LocalDateTime.of(2017, 11, 5, 11, 0).atZone(setting.getLocalZoneId());

        // schedule from Monday 00:00 to 09:00 am
        long expectedBegin = LocalDateTime.of(2017, 11, 6, 0, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 6, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowOutside() {
        // 6:00 pm -9:00 am
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)), LocalTime.of(18, 0),
                LocalTime.of(9, 0));

        // test between the two windows. i.e.between 9:00am - 6:00pm
        ZonedDateTime now = LocalDateTime.of(2017, 11, 6, 17, 0).atZone(setting.getLocalZoneId());
        long expectedBegin = LocalDateTime.of(2017, 11, 6, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 7, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin1st() {
        // 6:00 pm-9:00 am
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY)),
                LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test within 1st window, i.e. MONDAY between 0:00am-9:00am
        ZonedDateTime now = LocalDateTime.of(2017, 11, 6, 1, 0).atZone(setting.getLocalZoneId());

        // expected window 2017/11/5 18:0:0 (SUN) - 2017/11/6 9:00:59 (MON)
        long expectedBegin = LocalDateTime.of(2017, 11, 5, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 6, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin2ndNoGap() {
        // 6:00 pm-9:00 am
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)), LocalTime.of(18, 0),
                LocalTime.of(9, 0));

        // test within 2nd window, i.e. Monday between 6:00pm - 23:59pm
        ZonedDateTime now = LocalDateTime.of(2017, 11, 6, 19, 0).atZone(setting.getLocalZoneId());

        // schedule from now to end of 1st window next day
        long expectedBegin = LocalDateTime.of(2017, 11, 6, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 7, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin2ndWithGap() {
        // 6:00 pm-9:00 am
        DaysOfWeekTimeRangeSchedule setting = new DaysOfWeekTimeRangeSchedule(TIMEZONE,
                new TreeSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)), LocalTime.of(18, 0),
                LocalTime.of(9, 0));

        // test within 2nd window, i.e. between 6:00pm - 23:59pm on WEDNESDAY
        ZonedDateTime now = LocalDateTime.of(2017, 11, 8, 19, 0).atZone(setting.getLocalZoneId());

        // schedule from beginning to end of day WEDNESDAY
        long expectedBegin = LocalDateTime.of(2017, 11, 8, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 9, 0, 0).atZone(setting.getLocalZoneId()).toInstant()
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
