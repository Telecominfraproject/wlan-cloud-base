package com.telecominfraproject.wlan.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.equipment.SteerType;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum SteerType 
{
    steer_rsvd(1), 
    steer_deauth(2), 
    steer_11v(3),
    steer_perimeter(4),
    
    UNSUPPORTED(-1);

    private long id;

    private SteerType(long id) {
        this.id = id;
    }
    
    @JsonCreator
    public static SteerType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, SteerType.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(SteerType value) {
        return (UNSUPPORTED.equals(value));
    }
}
