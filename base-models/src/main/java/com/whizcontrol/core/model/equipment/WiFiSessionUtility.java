/**
 * 
 */
package com.whizcontrol.core.model.equipment;

import java.util.concurrent.TimeUnit;

/**
 * @author yongli
 *
 */
public class WiFiSessionUtility {
    /**
     * Decode a raw session id from equipment. On the equipment the 64 bit
     * integer consists of
     * 
     * <pre>
     *   32bit seconds 
     *   32bit microseconds
     * </pre>
     * 
     * @param rawValue
     * @return EPOCH in microsecond
     */
    public static long decodeWiFiAssociationId(long rawValue) {
        long tv_usec = (rawValue >>> (4 * 8)) & 0xFFFFFFFFL;
        long tv_sec = (rawValue & 0xFFFFFFFFL);
        return TimeUnit.SECONDS.toMicros(tv_sec) + tv_usec;
    }
    
    /**
     * Encode the wifi association id based on time value in ms
     * @param timeMs
     * @return encoded value
     */
    public static long encodeWiFiAssociationId(long timeMs) {
        long tv_sec = TimeUnit.MILLISECONDS.toSeconds(timeMs);
        long tv_usec = timeMs % TimeUnit.SECONDS.toMillis(1) * TimeUnit.MILLISECONDS.toMicros(1);
        return constructWifiSesssionId(tv_sec, tv_usec);
    }

    /**
     * Encode the wifi association id based on epoch time value in seconds and client MAC address
     * @param timeSeconds
     * @param clientMac
     * @return encoded value
     */
    public static long encodeWiFiAssociationId(long timeSeconds, long clientMac) {
        long tv_sec = timeSeconds;
        long tv_usec = clientMac % TimeUnit.SECONDS.toMicros(1);
        return constructWifiSesssionId(tv_sec, tv_usec);
    }

    private static long constructWifiSesssionId(long timeSeconds, long timeMicroSeconds) 
    {
        // Sanity check...
        if (timeSeconds > TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TimeUnit.DAYS.toSeconds(3650)) {
            throw new IllegalArgumentException("SessionID too far in the future. Seconds: " + timeSeconds);
        }

        return (timeMicroSeconds << (4*8)) | (timeSeconds&0xFFFFFFFFL);
    }
}
