package com.telecominfraproject.wlan.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.equipment.SecurityType;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum SecurityType
{
    OPEN,
    RADIUS,
    PSK,
    SAE,
    
    UNSUPPORTED;
    
    @JsonCreator
    public static SecurityType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, SecurityType.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(SecurityType value) {
        return (UNSUPPORTED.equals(value));
    }
}

