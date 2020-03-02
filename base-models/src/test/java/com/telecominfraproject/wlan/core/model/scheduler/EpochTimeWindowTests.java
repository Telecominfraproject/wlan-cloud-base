package com.telecominfraproject.wlan.core.model.scheduler;

import static org.junit.Assert.*;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.scheduler.TimeWindowValue;

public class EpochTimeWindowTests {

    @Test
    public void testCompare() {
        TimeWindowValue left = new TimeWindowValue();
        TimeWindowValue right = new TimeWindowValue(-1, -1);
        assertNotEquals(left, right);
        assertTrue(0 == left.compareTo(right));
    }
}
