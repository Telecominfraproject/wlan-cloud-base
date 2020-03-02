package com.telecominfraproject.wlan.core.model.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.utils.DateTimeUtils;

public class DateTimeUtilsTests {

    private static final Logger LOG = LoggerFactory.getLogger(DateTimeUtilsTests.class);

    @Test
    public void testFormatter() {
        long currentTime = System.currentTimeMillis();
        String formattedValue = DateTimeUtils.formatTimestamp(currentTime);
        LOG.debug("Format {} to {}", currentTime, formattedValue);
        assertNotNull(formattedValue);
        formattedValue = DateTimeUtils.formatTimestamp(null);
        assertNull(formattedValue);
    }

}
