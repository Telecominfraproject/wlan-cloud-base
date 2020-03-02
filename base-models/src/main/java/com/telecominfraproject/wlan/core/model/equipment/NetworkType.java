/**
 * 
 */
package com.telecominfraproject.wlan.core.model.equipment;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.equipment.NetworkType;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

/**
 * @author ekeddy
 *
 */
public enum NetworkType {
    AP(0), 
    ADHOC(1), 
    UNSUPPORTED(-1);

    private final int id;
    private static final Map<Long, NetworkType> ELEMENTS = new HashMap<>();

    NetworkType(int id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static NetworkType getById(long enumId) {
        if (ELEMENTS.isEmpty()) {
            synchronized (ELEMENTS) {
                if (ELEMENTS.isEmpty()) {
                    // initialize elements map
                    for (NetworkType met : NetworkType.values()) {
                        ELEMENTS.put(met.getId(), met);
                    }
                }
            }
        }
        return ELEMENTS.get(enumId);
    }

    @JsonCreator
    public static NetworkType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, NetworkType.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(NetworkType value) {
        return (UNSUPPORTED.equals(value));
    }
}
