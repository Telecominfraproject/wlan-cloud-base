package com.telecominfraproject.wlan.core.model.json.interfaces;
/**
 * Marker interface that tells if current object can provide a timestamp from its source data (i.e. timestamp for stats report used to generate a ServiceMetric)
 * @author mikehansen1970
 *
 */
public interface HasSourceTimestamp {

    /**
     * @return the timestamp of the source data for this object (i.e. for the stats that were used for a ServiceMetric)
     */
    public long getSourceTimestampMs();
}
