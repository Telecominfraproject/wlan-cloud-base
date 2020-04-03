package com.telecominfraproject.wlan.core.model.entity;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum CountryCode {

    usa(0),
    ca(1),
    integration(1000),
    
    UNSUPPORTED(-1);

    private final int id;
    private static final Map<Integer, CountryCode> ELEMENTS = new HashMap<>();

    private CountryCode(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static CountryCode getById(int enumId) {
        if (ELEMENTS.isEmpty()) {
            synchronized (ELEMENTS) {
                if (ELEMENTS.isEmpty()) {
                    //initialize elements map
                    for(CountryCode met : CountryCode.values()) {
                        ELEMENTS.put(met.getId(), met);
                    }
                }
            }
        }
        return ELEMENTS.get(enumId);
    }
    @JsonCreator
    public static CountryCode getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, CountryCode.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(CountryCode value) {
        return UNSUPPORTED.equals(value);
    }
}
