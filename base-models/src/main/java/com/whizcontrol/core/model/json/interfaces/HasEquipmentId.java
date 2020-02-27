package com.whizcontrol.core.model.json.interfaces;

/**
 * Marker interface that tells if current object can provide equipmentId for which it belongs to 
 * @author dtop
 *
 */
public interface HasEquipmentId {
    
    /**
     * @return equipmentId for which this object belongs to
     */
    long getEquipmentId();
}
