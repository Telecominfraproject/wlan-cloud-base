package com.telecominfraproject.wlan.core.model.equipment;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum SourceType {

    auto(0L),
    manual(1L),
    profile(2L),
    
    UNSUPPORTED(-1L);

    private final long id;
    private static final Map<Long, SourceType> ELEMENTS = new HashMap<>();

    private SourceType(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    public static SourceType getById(long enumId) {
        if (ELEMENTS.isEmpty()) {
            synchronized (ELEMENTS) {
                if (ELEMENTS.isEmpty()) {
                    //initialize elements map
                    for(SourceType met : SourceType.values()) {
                        ELEMENTS.put(met.getId(), met);
                    }
                }
            }
        }
        return ELEMENTS.get(enumId);
    }
    
    @JsonCreator
    public static SourceType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, SourceType.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(SourceType value) {
        return UNSUPPORTED.equals(value);
    }
}
