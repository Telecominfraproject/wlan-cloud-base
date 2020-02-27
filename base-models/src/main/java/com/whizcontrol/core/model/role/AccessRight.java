/**
 * 
 */
package com.whizcontrol.core.model.role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * Control access to object in the system.
 * 
 * @author yongli
 *
 */
public enum AccessRight {
    /**
     * Can do any thing
     */
    ALL,

    /**
     * View Equipment (AP)
     */
    VIEW_EQUIPMENT,

    /**
     * View Wi-Fi Device
     */
    VIEW_DEVICE,
    /**
     * View Alarm
     */
    VIEW_ALARM,
    /**
     * Control equipment, reboot
     */
    CONTROL_EQUIPMENT,
    /**
     * Reserved for unknown enumerated value
     */
    UNSUPPORTED;

    @JsonCreator
    public static AccessRight getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, AccessRight.class, AccessRight.UNSUPPORTED);
    }

    public static boolean isUnsupported(AccessRight value) {
        return AccessRight.UNSUPPORTED.equals(value);
    }
}
