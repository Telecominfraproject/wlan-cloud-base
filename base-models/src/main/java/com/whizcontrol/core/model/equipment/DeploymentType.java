package com.whizcontrol.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * This is used for calculate RF distance
 * 
 * @author erikvilleneuve
 *
 */
public enum DeploymentType 
{
    DESK,
    CEILING,
    UNSUPPORTED;
    
    @JsonCreator
    public static DeploymentType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, DeploymentType.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(DeploymentType value) {
        return (UNSUPPORTED.equals(value));
    }
}
