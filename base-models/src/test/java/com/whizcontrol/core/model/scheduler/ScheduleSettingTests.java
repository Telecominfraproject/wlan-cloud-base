package com.whizcontrol.core.model.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whizcontrol.core.model.scheduler.ScheduleSetting;

public class ScheduleSettingTests {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduleSettingTests.class);

    @Test
    public void testZone() {
        Set<String> zoneSet = ScheduleSetting.allZoneIds();
        LOG.debug("Supported time zone: {}", zoneSet);
        assertNotNull(zoneSet);
        assertFalse(zoneSet.isEmpty());
    }
}
