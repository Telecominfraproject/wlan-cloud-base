package com.telecominfraproject.wlan.core.model.equipment;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.equipment.AutoOrManualValue;

public class AutoOrManualIntegerTests 
{
    @Test
    public void testBasicEquals()
    {
        assertEquals(AutoOrManualValue.createAutomaticInstance(-90), AutoOrManualValue.createAutomaticInstance(-90));
    }

}
