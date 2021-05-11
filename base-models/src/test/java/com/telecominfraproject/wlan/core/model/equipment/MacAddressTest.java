package com.telecominfraproject.wlan.core.model.equipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacAddressTest {
    private static final Logger LOG = LoggerFactory.getLogger(MacAddressTest.class);

    @Test
    public void testMacADddressPrintout() {
        MacAddress mac = new MacAddress(123L);

        LOG.debug("new MacAddress(123L): {}", mac);

        final String serializedStr = "{\"_type\":\"MacAddress\",\"address\":\"AAAAAAB7\",\"macAddressAsString\":\"00:00:00:00:00:7b\"}";

        MacAddress deserialized = MacAddress.fromString(serializedStr, MacAddress.class);

        assertEquals(mac, deserialized);
    }

    @Test
    public void testInvalidValidMac() {
        final String serializedStr = "{\"_type\":\"MacAddress\",\"address\":\"qGZ/Ew==\"}";
        MacAddress mac = MacAddress.fromString(serializedStr, MacAddress.class);
        assertNull(mac.getAddressAsLong());
    }

    @Test
    /*
     * THere was an issue when we had a null payload, printing the JSON would
     * yield an NPE.
     */
    public void testPrintWhenNull() {
        MacAddress mac = new MacAddress();
        LOG.debug("new MacAddress(): {}", mac);
    }

    @Test
    public void testOuiStuff() {
        assertEquals("12abcd", MacAddress.ouiFromLowerCaseString("12:ab:cd:12:ab:cd", true));
    }

    @Test
    public void testHashCodeConflictsNewMethod() {
        long firstBase = new MacAddress("74:9C:E3:01:02:81").getAddressAsLong();
        long secondBase = new MacAddress("74:9C:E3:01:01:A0").getAddressAsLong();

        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                Map<MacAddress, Integer> myMap = new HashMap<>();

                myMap.put(new MacAddress(firstBase + i), 1);
                myMap.put(new MacAddress(secondBase + j), 2);

                // We make sure there was no internal collisions
                assertEquals(2, myMap.size());
            }
        }
    }

    @Test
    public void testGroupOrGlobal() {
        MacAddress realMac = new MacAddress("0C:47:3D:F4:9A:D8");
        MacAddress fakeMac = new MacAddress("0E:47:3D:F4:9A:D8");
        MacAddress another = new MacAddress("fa:8f:ca:67:00:00");
        assertFalse(fakeMac.isGlobalAddress());
        assertTrue(realMac.isGlobalAddress());
        assertFalse(another.isGlobalAddress());
    }

    @Test
    public void testBasicConversion() throws Exception {
        byte address[] = { 1, 2, 3, 4, 5, 6 };
        MacAddress macAddress = new MacAddress(address);
        assertEquals((Long) 1108152157446L, macAddress.getAddressAsLong());
    }

    @Test
    public void testTwoWayConversion() throws Exception {
        byte address[] = { 1, 2, 3, 4, 5, 6 };
        MacAddress macAddress = new MacAddress(address);
        MacAddress fromLongAddress = new MacAddress(macAddress.getAddressAsLong());
        assertEquals(macAddress, fromLongAddress);
    }

    @Test
    public void testSerialization() throws Exception {
        MacAddress test = new MacAddress(new byte[] { 1, 2, 3, 4, 5, 6 });
        MacAddress recomposed = (MacAddress) MacAddress.fromString(test.toString(), MacAddress.class);
        assertEquals(test, recomposed);
    }

    @Test
    public void testStringRepresentation() {
        MacAddress test = new MacAddress(new byte[] { 1, 2, 3, 4, 5, 6 });
        assertEquals("01:02:03:04:05:06", test.getAddressAsString());
    }

    @Test
    public void testMacLong() {
        byte address[] = { 1, 2, 3, 4, 5, 6 };
        MacAddress macAddress = new MacAddress(address);
        MacAddress fromLong = new MacAddress(macAddress.getAddressAsLong());
        assertEquals(macAddress, fromLong);
    }

    @Test
    public void testBaseMacConversion() {
        MacAddress expectedAddress = new MacAddress("00:2a:f7:7a:1e:a0");
        BigInteger bigInterger = new BigInteger("002af77a1eaf", 16);
        BigInteger diff = new BigInteger("15");
        BigInteger resulting = bigInterger.add(diff.negate());
        MacAddress actual = new MacAddress(resulting.longValue());
        assertEquals(expectedAddress.getAddressAsLong(), actual.getAddressAsLong());
    }

    @Test
    public void testConvertingFromOneTypeToAnother() {
        MacAddress initialAddress = new MacAddress("ff:ff:ff:ff:ff:ff");
        MacAddress addressBuiltFromLong = new MacAddress(initialAddress.getAddressAsLong());
        MacAddress addressFromString = new MacAddress(addressBuiltFromLong.getAddressAsString());
        assertEquals(initialAddress, addressFromString);
    }

    @Test
    public void testBasicEquals() {
        MacAddress first = new MacAddress(1L);
        MacAddress equalToFirst = new MacAddress(1L);
        MacAddress notEqualToFirst = new MacAddress(2L);

        assertEquals(first, equalToFirst);
        assertNotEquals(first, notEqualToFirst);
    }
    
    @Test
    public void testSetAddress() {
        MacAddress nullCheck = new MacAddress();
        nullCheck.setAddress(null);
        assertNull(nullCheck.getAddress());
        
        MacAddress realValue = new MacAddress();
        realValue.setAddress(new byte[] { 1, 2, 3, 4, 5, 6 });
        assertNotNull(realValue.getAddress());
    }
    
    @Test
    public void testGetAddressAsLong()
    {
        MacAddress macAddress = new MacAddress("00:2a:f7:7a:1e:a0");
        assertNotNull(macAddress.getAddressAsLong());
        
        MacAddress otherMacAddress = new MacAddress(new byte[] { 1, 2, 3, 4, 5});
        assertNull(otherMacAddress.getAddressAsLong());
    }
    
    @Test
    public void testGetAddressAsString() {
        MacAddress macAddress = new MacAddress("00:2a:f7:7a:1e:a0");
        assertNotNull(macAddress.getAddressAsString());
    }
    
    @Test
    public void testGetAsLowerCaseString() {
        MacAddress macAddress = new MacAddress("00:2a:f7:7a:1e:a0");
        assertNotNull(macAddress.getAsLowerCaseString().toCharArray());
    }
    
    @Test
    public void testHashCode()
    {
        MacAddress macAddress = new MacAddress("00:2a:f7:7a:1e:a0");
        
        assertNotEquals(macAddress.hashCode(), 0);
    }
    
    @Test
    public void testStringToByteArray()
    {
        MacAddress macAddress = new MacAddress();
        macAddress.setAddressAsString(null);
        assertNull(macAddress.getAddress());
        
        macAddress.setAddressAsString("00:2a:f7:7a:1e:a0");
        assertNotNull(macAddress.getAddress());
        
        try {
            macAddress.setAddressAsString("00:2a:f7:7a:1e:a0:b0:f1");
            fail("expected exception.");
        } catch (IllegalArgumentException e)
        {
            //expected it
        }
        try {
            macAddress.setAddressAsString("00:2a:f7:7a:1esdafsat:a0");
            fail("expected exception.");
        } catch (IllegalArgumentException e)
        {
            //expected it
        }
    }
    
    @Test
    public void testOuiFromLowerCaseString()
    {
        assertNotNull(MacAddress.ouiFromLowerCaseString("00:2a:f7:7a:1e:a0", true));
        assertNotNull(MacAddress.ouiFromLowerCaseString("00:2a:f7:7a:1e:a0", false));
    }
    
    @Test
    public void testToOuiString()
    {
        MacAddress macAddress = new MacAddress("00:2a:f7:7a:1e:a0");
        assertNotNull(macAddress.toOuiString());
        
        MacAddress nullCheck = new MacAddress();
        nullCheck.setAddressAsString(null);
        assertNull(nullCheck.toOuiString());
    }
    
    @Test
    public void testConvertMacStringToLongValue()
    {
        assertNotNull(MacAddress.convertMacStringToLongValue("00:2a:f7:7a:1e:a0"));
        assertNull(MacAddress.convertMacStringToLongValue(null));
    }

}
