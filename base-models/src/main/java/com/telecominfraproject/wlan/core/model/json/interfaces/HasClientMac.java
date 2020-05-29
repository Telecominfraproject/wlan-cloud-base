package com.telecominfraproject.wlan.core.model.json.interfaces;

import com.telecominfraproject.wlan.core.model.equipment.MacAddress;

/**
 * Marker interface that tells if current object can provide clientMacAddress for which it belongs to 
 * @author dtop
 *
 */
public interface HasClientMac {
    
    /**
     * @return clientMacAddress for which this object belongs to
     */
    MacAddress getClientMacAddress();
}
