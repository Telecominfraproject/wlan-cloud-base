/**
 *
 */
package com.telecominfraproject.wlan.core.model.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
    
    public static final TimeZone TZ_GMT = TimeZone.getTimeZone("GMT");

    public static int getDayOfYear(long timestampMs) {
        //all the date-time operations on the server are always in GMT
        Calendar calendar = Calendar.getInstance(TZ_GMT);
        calendar.setTimeInMillis(timestampMs);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        return dayOfYear;
    }

    public static int getYear(long timestampMs) {
        //all the date-time operations on the server are always in GMT
        Calendar calendar = Calendar.getInstance(TZ_GMT);
        calendar.setTimeInMillis(timestampMs);
        int year = calendar.get(Calendar.YEAR);

        return year;
    }

    public static int getMonth(long timestampMs) {
        //all the date-time operations on the server are always in GMT
        Calendar calendar = Calendar.getInstance(TZ_GMT);
        calendar.setTimeInMillis(timestampMs);
        int month = calendar.get(Calendar.MONTH);

        return month;
    }

    public static int getWeekOfYear(long timestampMs) {
        //all the date-time operations on the server are always in GMT
        Calendar calendar = Calendar.getInstance(TZ_GMT);
        calendar.setTimeInMillis(timestampMs);
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

        return weekOfYear;
    }

}
