/**
 * 
 */
package com.telecominfraproject.wlan.datastore.inmemory;

import java.util.concurrent.TimeUnit;

import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * @author yongli
 *
 */
public abstract class BaseInMemoryDatastore {

    /**
     * Wait to up to 5 seconds to catch up with current last mod
     */
    private static final long NEXT_LASTMOD_WAIT_THRESHOLD = 5;

    /**
     * Constructor
     */
    protected BaseInMemoryDatastore() {
    }

    /**
     * Create the last modified timestamp based on the current one
     * 
     * @param currentLastModTs
     * @return new last modified TS
     */
    public static long getNewLastModTs(long currentLastModTs) {
        long result = System.currentTimeMillis();
        while (result <= currentLastModTs) {
            long diff = currentLastModTs - result;
            if (diff > TimeUnit.SECONDS.toMillis(NEXT_LASTMOD_WAIT_THRESHOLD)) {
                throw new IllegalArgumentException("Existing last modified TS is in the future");
            }
            if (diff > 0) {
                // pause till we have a time great than current lastMod
                try {
                    Thread.sleep(diff + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GenericErrorException("Unable to generate the new last modified TS", e);
                }
            }
            result = System.currentTimeMillis();
        }
        return result;
    }
}
