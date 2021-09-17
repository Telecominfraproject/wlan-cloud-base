
package com.telecominfraproject.wlan.core.model.equipment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum LedStatus {
    led_on(1), 
    led_off(2), 
    led_blink(3), 
    UNKNOWN(-1);
	
	private final int id;
	
	LedStatus(int id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
    @JsonCreator
    public static LedStatus getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, LedStatus.class, UNKNOWN);
    }
}
