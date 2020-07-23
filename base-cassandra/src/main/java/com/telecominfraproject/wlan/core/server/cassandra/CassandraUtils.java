package com.telecominfraproject.wlan.core.server.cassandra;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dtop
 *
 */
public class CassandraUtils {
	
    private static final Logger LOG = LoggerFactory.getLogger(CassandraUtils.class);


	/**
	 * @param c - collection of values
	 * @return string containing bind variable placeholders - one for each element of the supplied collection. e.g. " (?,?,?) "
	 */
	public static String getBindPlaceholders(Collection<?> c) {
        StringBuilder strb = new StringBuilder(100);
        strb.append(" (");
        for (int i = 0; i < c.size(); i++) {
            strb.append("?");
            if (i < c.size() - 1) {
                strb.append(",");
            }
        }
        strb.append(") ");

        return strb.toString();
	}
	
	/**
	 * @param collection
	 * @return true if the collection is not empty
	 */
	public static boolean isPresent( Collection<?> c) {
		return c!=null && !c.isEmpty();
	}


	private static final TimeZone tz = TimeZone.getTimeZone("GMT");

	public static int getDayOfYear(long timestampMs) {
		//all the date-time operations on the server are always in GMT
		Calendar calendar = Calendar.getInstance(tz);
		calendar.setTimeInMillis(timestampMs);
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

		return dayOfYear;
	}

	public static Set<Integer> calculateDaysOfYear(long fromTime, long toTime) {
		if(fromTime > toTime) {
			throw new IllegalArgumentException("From time must be before To time");
		}

		//all the date-time operations on the server are always in GMT
		Calendar calendarFrom = Calendar.getInstance(tz);
		calendarFrom.setTimeInMillis(fromTime);
		int fromDayOfYear = calendarFrom.get(Calendar.DAY_OF_YEAR);
		int fromYear = calendarFrom.get(Calendar.YEAR);  

		Calendar calendarTo = Calendar.getInstance(tz);
		calendarTo.setTimeInMillis(toTime);
		int toDayOfYear = calendarTo.get(Calendar.DAY_OF_YEAR);
		int toYear = calendarTo.get(Calendar.YEAR);  
		
		Set<Integer> days = new HashSet<>();
		//build a set of daysOfYear - it may contain extra days, they will be ignored because there are other filters used in addition to this synthetic one
		if(fromDayOfYear <= toDayOfYear && fromYear == toYear) {
			//simple case - the from and to dates are in the same year
			for(int i = fromDayOfYear; i<=toDayOfYear; i++) {
				days.add(i);
			}
		} else if(fromDayOfYear > toDayOfYear && fromYear == toYear - 1) {
			//the from and to dates are in the adjacent years
			for(int i = fromDayOfYear; i<=366; i++) {
				days.add(i);
			}
			
			for(int i = 1; i<=toDayOfYear; i++) {
				days.add(i);
			}
		} else {
			//the from and to dates are more than a year apart
			for(int i = 1; i<=366; i++) {
				days.add(i);
			}
			
		}

		LOG.trace("Calendar from doy = {} y = {}", fromDayOfYear , fromYear);
		LOG.trace("Calendar to doy = {} y = {} ", toDayOfYear, toYear);
		LOG.trace("days = {}", days);
		
		return days;
	}

	public static void main(String[] args) {
		long fromTime = 1579357425000L; // Saturday, January 18, 2020 2:23:45 PM
		long toTime = 1600611825000L; //Sunday, September 20, 2020 2:23:45 PM
		calculateDaysOfYear(fromTime , toTime );

		fromTime = 1579357425000L; // Saturday, January 18, 2020 2:23:45 PM
		toTime = 1579359634000L; //Saturday, January 18, 2020 3:00:34 PM
		calculateDaysOfYear(fromTime , toTime );

		fromTime = 1576008034000L; // Tuesday, December 10, 2019 8:00:34 PM
		toTime = 1579359634000L; //Saturday, January 18, 2020 3:00:34 PM
		calculateDaysOfYear(fromTime , toTime );

		fromTime = 1453320034000L; // Wednesday, January 20, 2016 8:00:34 PM
		toTime = 1579359634000L; //Saturday, January 18, 2020 3:00:34 PM
		calculateDaysOfYear(fromTime , toTime );

	}
}
