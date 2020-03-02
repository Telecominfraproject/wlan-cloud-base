/**
 *
 */
package com.telecominfraproject.wlan.core.model.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yongli
 *
 */
public class DateTimeUtils {

    /**
     * Date time format pattern 2018-12-05T03:00:00.000Z
     */
    private static final String TS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    /**
     *
     */
    private final static DateFormat TS_FORMATTER = new SimpleDateFormat(TS_PATTERN);

    private DateTimeUtils() {
    }

    /**
     * Format a timestamp to string use {@value #TS_PATTERN}
     *
     * @param timestamp
     * @return null if timestamp is null
     */
    public static String formatTimestamp(Long timestamp) {
        if (timestamp != null) {
            return TS_FORMATTER.format(new Date(timestamp.longValue()));
        }
        return null;
    }
}
