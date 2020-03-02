package com.telecominfraproject.wlan.core.model.equipment;

import static org.junit.Assert.*;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.equipment.WiFiSessionUtility;

public class WifiSessionUtilityTests 
{
    @Test
    public void testBasicConversion()
    {
        long raw = 1000;
        assertEquals(raw, WiFiSessionUtility.decodeWiFiAssociationId(WiFiSessionUtility.encodeWiFiAssociationId(raw / 1000)));
    }

}
