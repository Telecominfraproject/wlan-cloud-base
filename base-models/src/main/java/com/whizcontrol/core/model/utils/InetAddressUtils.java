/**
 * 
 */
package com.whizcontrol.core.model.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.whizcontrol.server.exceptions.GenericErrorException;

/**
 * @author yongli
 *
 */
public class InetAddressUtils {

    private InetAddressUtils() {
    }

    public static String encodeToString(InetAddress addr) {
        return addr.getHostAddress();
    }

    public static InetAddress decodeFromString(String addr) {
        try {
            return InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            throw new GenericErrorException("Failed to translate InetAddress from " + addr, e);
        }
    }

}
