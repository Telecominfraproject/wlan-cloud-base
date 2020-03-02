package com.telecominfraproject.wlan.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum OBSSHopMode 
{
    NON_WIFI(1),
    NON_WIFI_AND_OBSS(2),
    UNSUPPORTED(-1);
    
    private int code;
    
    private OBSSHopMode(int code)
    {
        this.code = code;        
    }

    @JsonCreator
    public static OBSSHopMode getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, OBSSHopMode.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(OBSSHopMode value) {
        return (UNSUPPORTED.equals(value));
    }

}
