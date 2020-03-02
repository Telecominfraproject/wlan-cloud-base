package com.telecominfraproject.wlan.core.model.utils;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.utils.InetAddressUtils;

public class InetAddressUtilsTest {
    @Test
    public void testBasic() throws Exception {
        final InetAddress addr = InetAddress.getByName("206.80.249.112");
        String str = InetAddressUtils.encodeToString(addr);

        assertEquals("206.80.249.112", str);
    }

}
