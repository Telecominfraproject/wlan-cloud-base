package com.whizcontrol.core.model.equipment;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Test;

public class NetworkInterfaceTypeTests {

    @Test
    public void testGetInterfaceType() {
        Map<MacAddress, NetworkInterfaceType> testValues = new TreeMap<>();
        for (int i = 0x00; i <= 0x1F; ++i) {
            if (i <= 0x0D) {
                testValues.put(MacAddress.valueOf(0x749ce3101020L + i), NetworkInterfaceType.RADIO_2G);
            } else if (i == 0x0F) {
                testValues.put(MacAddress.valueOf(0x749ce3101020L + i), NetworkInterfaceType.ETHERNET);
            } else if (i >= 0x10 && i <= 0x1D) {
                testValues.put(MacAddress.valueOf(0x749ce3101020L + i), NetworkInterfaceType.RADIO_5G);
            } else {
                testValues.put(MacAddress.valueOf(0x749ce3101020L + i), NetworkInterfaceType.RESERVED);
            }
        }
        testValues.put(MacAddress.valueOf(0x100000000000L), NetworkInterfaceType.UNSUPPORTED);

        for (Entry<MacAddress, NetworkInterfaceType> entry : testValues.entrySet()) {
            assertEquals(entry.getKey().getAsLowerCaseString(), entry.getValue(),
                    NetworkInterfaceType.interfaceType(entry.getKey()));
        }
    }

}
