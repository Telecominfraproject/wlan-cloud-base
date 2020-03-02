/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.scheduler.DailyTimeRangeSchedule;
import com.telecominfraproject.wlan.core.model.scheduler.EpochTimeWindow;
import com.telecominfraproject.wlan.core.model.scheduler.ScheduleSetting;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;

/**
 * @author yongli
 *
 */
public class DailyTimeRangeScheduleTests {

    private static final String TIMEZONE = ZoneId.systemDefault().getId();
    private static final Logger LOG = LoggerFactory.getLogger(DailyTimeRangeScheduleTests.class);

    @Test
    public void testDataValidation() {
        Duration duration = Duration.ofHours(1);

        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule("badTZ", LocalTime.of(0, 0), LocalTime.of(2, 0));
        checkInvalidSetting(setting, "invalid TZ", duration);

        setting = new DailyTimeRangeSchedule(TIMEZONE, null, LocalTime.of(0, 59));
        checkInvalidSetting(setting, "minimum duration", duration);

        setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(23, 40), LocalTime.of(0, 19));
        checkInvalidSetting(setting, "minimum duration", duration);

        // verify normal setting
        setting = new DailyTimeRangeSchedule(TIMEZONE, null, LocalTime.of(1, 0));
        setting.validateSetting(duration);

        // verify revert setting
        setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(23, 0), LocalTime.MIDNIGHT);
        setting.validateSetting(duration);
    }

    @Test
    public void testJSONSerialization() {
        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(18, 0), LocalTime.of(9, 0));
        String jsonString = setting.toPrettyString();
        LOG.debug("Testing {}", jsonString);
        ScheduleSetting result = BaseJsonModel.fromString(jsonString, ScheduleSetting.class);
        assertEquals(setting, result);
        assertFalse(setting.isEmpty());
    }

    @Test
    public void testNormalWindowAfter() {
        // 9:00 am-12:00 pm
        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(9, 0), LocalTime.of(12, 0));

        // test after window end 12:00pm
        ZonedDateTime now = LocalDateTime.of(2017, 11, 7, 22, 0).atZone(setting.getLocalZoneId());
        // schedule to next day, 2017/11/8 9:00:00 - 2017/11/8/12:00:59
        long expectedBegin = LocalDateTime.of(2017, 11, 8, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 8, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testNormalWindowBefore() {
        // 9:00 am-12:00 pm
        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(9, 0), LocalTime.of(12, 0));

        // test before window
        ZonedDateTime now = LocalDateTime.of(2017, 11, 8, 1, 0).atZone(setting.getLocalZoneId());
        // schedule to 2017/11/8 9:00:00 - 2017/11/8 12:00:59
        long expectedBegin = LocalDateTime.of(2017, 11, 8, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 8, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testNormalWindowWithin() {
        // 9:00 am-12:00 pm
        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(9, 0), LocalTime.of(12, 0));

        // test within window
        ZonedDateTime now = LocalDateTime.of(2017, 11, 8, 10, 0).atZone(setting.getLocalZoneId());
        // schedule to 2017/11/8 9:00:00-2017/11/9 21:00:59 to end
        long expectedBegin = LocalDateTime.of(2017, 11, 8, 9, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 8, 12, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowOutside() {
        // 6:00 pm -9:00 am
        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test outside the two windows. i.e.between 9:00am - 6:00pm
        ZonedDateTime now = LocalDateTime.of(2017, 11, 8, 17, 0).atZone(setting.getLocalZoneId());
        // schedule to 2nd and extend to next day
        // 2017/11/8 18:00:00 - 2017/11/9 09:00:59
        long expectedBegin = LocalDateTime.of(2017, 11, 8, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 9, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;
        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin1st() {
        // 6:00 pm-9:00 am
        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test between 1st window, i.e. between 0:00am-9:00am
        ZonedDateTime now = LocalDateTime.of(2017, 11, 8, 1, 0).atZone(setting.getLocalZoneId());

        // schedule to end of 1st window.
        // i.e. 2017/11/7 18:00:00 - 2017/11/8 09:00:59
        long expectedBegin = LocalDateTime.of(2017, 11, 7, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 8, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    @Test
    public void testReverseWindowWithin2nd() {
        // 6:00 pm-9:00 am
        DailyTimeRangeSchedule setting = new DailyTimeRangeSchedule(TIMEZONE, LocalTime.of(18, 0), LocalTime.of(9, 0));

        // test within 2nd window, i.e. between 6:00pm - 23:59pm
        ZonedDateTime now = LocalDateTime.of(2017, 11, 8, 19, 0).atZone(setting.getLocalZoneId());

        // schedule to now - end of next day 1st window
        long expectedBegin = LocalDateTime.of(2017, 11, 8, 18, 0).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli();
        long expectedEnd = LocalDateTime.of(2017, 11, 9, 9, 1).atZone(setting.getLocalZoneId()).toInstant()
                .toEpochMilli() - 1;

        EpochTimeWindow result = setting.getNextWindows(now.toInstant().toEpochMilli());
        assertNotNull(result);
        assertEquals(expectedBegin, result.getBeginTime());
        assertEquals(expectedEnd, result.getEndTime());
    }

    private void checkInvalidSetting(DailyTimeRangeSchedule setting, String check, Duration duration) {
        try {
            setting.validateSetting(duration);
            fail("failed " + check + ": " + setting);
        } catch (DsDataValidationException exp) {
            // pass
            LOG.debug("pass {}: {}", check, exp.getLocalizedMessage());
        }
    }

}
