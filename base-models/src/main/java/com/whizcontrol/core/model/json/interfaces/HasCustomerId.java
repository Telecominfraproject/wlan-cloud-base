package com.whizcontrol.core.model.json.interfaces;

/**
 * Marker interface that tells if current object can provide customerId for which it belongs to 
 * @author dtop
 *
 */
public interface HasCustomerId {
    
    /**
     * @return customerId for which this object belongs to
     */
    int getCustomerId();
}
