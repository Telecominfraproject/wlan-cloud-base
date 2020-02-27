/**
 * 
 */
package com.whizcontrol.core.model.equipment;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * @author ekeddy
 *
 */
public enum Toggle {
    off(0), 
    on(1), 
    
    UNSUPPORTED(-1);

    private final int id;
    private static final Map<Long, Toggle> ELEMENTS = new HashMap<>();

    Toggle(int id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static Toggle getById(long enumId) {
        if (ELEMENTS.isEmpty()) {
            synchronized (ELEMENTS) {
                if (ELEMENTS.isEmpty()) {
                    // initialize elements map
                    for (Toggle met : Toggle.values()) {
                        ELEMENTS.put(met.getId(), met);
                    }
                }
            }
        }
        return ELEMENTS.get(enumId);
    }

    @JsonCreator
    public static Toggle getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, Toggle.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(Toggle value) {
        return (UNSUPPORTED.equals(value));
    }
}
