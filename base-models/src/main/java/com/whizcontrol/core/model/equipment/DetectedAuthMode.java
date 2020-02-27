package com.whizcontrol.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

public enum DetectedAuthMode 
{
    OPEN(0),
    WEP(1),
    WPA(2),
    UNKNOWN(3),
    UNSUPPORTED(-1);
    
    private final int id;
    
    private DetectedAuthMode(int id)
    {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @JsonCreator
    public static DetectedAuthMode getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, DetectedAuthMode.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(DetectedAuthMode value) {
        return (UNSUPPORTED.equals(value));
    }

    
}
