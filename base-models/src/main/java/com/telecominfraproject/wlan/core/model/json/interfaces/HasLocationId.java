package com.telecominfraproject.wlan.core.model.json.interfaces;

/**
 * Marker interface that tells if current object can provide locationId for which it belongs to 
 * @author dtop
 *
 */
public interface HasLocationId {
    
    /**
     * @return locationId for which this object belongs to
     */
    long getLocationId();
}
