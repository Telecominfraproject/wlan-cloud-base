package com.telecominfraproject.wlan.core.model.utils;

/**
 * Generate long id from current time using milliseconds and nanoseconds
 * @author dtop
 *
 */
public class TimeBasedId {

	public static final long generateIdFromTimeNanos() {
		return (System.currentTimeMillis() << 20) | (System.nanoTime() & ~0b1111111111111111111111111111111111111111111100000000000000000000L ) ;
	}

}
