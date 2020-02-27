package com.whizcontrol.core.model.json.interfaces;

/**
 * Marker interface that tells if current object can provide a timestamp of when it was produced (i.e. eventTimestampMs)
 * @author dtop
 *
 */
public interface HasProducedTimestamp {
    
    /**
     * @return timestamp of when this object was produced (i.e. eventTimestampMs)
     */
    long getProducedTimestampMs();
}
