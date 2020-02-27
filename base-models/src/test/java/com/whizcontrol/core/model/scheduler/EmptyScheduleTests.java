package com.whizcontrol.core.model.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whizcontrol.core.model.json.BaseJsonModel;
import com.whizcontrol.core.model.scheduler.EmptySchedule;
import com.whizcontrol.core.model.scheduler.ScheduleSetting;

public class EmptyScheduleTests {

    private static final Logger LOG = LoggerFactory.getLogger(EmptyScheduleTests.class);

    @Test
    public void testJSONSerialization() {
        EmptySchedule setting = new EmptySchedule();
        String jsonString = setting.toPrettyString();
        LOG.debug("Testing {}", jsonString);
        ScheduleSetting result = BaseJsonModel.fromString(jsonString, ScheduleSetting.class);
        assertEquals(setting, result);
        assertTrue(setting.isEmpty());
    }
}
