package com.telecominfraproject.wlan.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.equipment.ChannelHopReason;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum ChannelHopReason {
    RadarDetected, HighInterference, UNSUPPORTED;

    @JsonCreator
    public static ChannelHopReason getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, ChannelHopReason.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(ChannelHopReason value) {
        return UNSUPPORTED.equals(value);
    }
}