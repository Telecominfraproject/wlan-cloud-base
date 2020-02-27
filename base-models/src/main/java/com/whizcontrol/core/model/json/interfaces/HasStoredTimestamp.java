package com.whizcontrol.core.model.json.interfaces;

/**
 * Marker interface that tells if current object can provide a timestamp of when it was stored in the database (i.e. createdTimestampMs)
 * @author dtop
 *
 */
public interface HasStoredTimestamp {

    /**
     * @return timestamp of when current object was stored in the database (i.e. createdTimestampMs)
     */
    long getStoredTimestampMs();
}
