/**
 * 
 */
package com.whizcontrol.core.model.service;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * Different type of Cloud Service
 * 
 * @author yongli
 *
 */
public enum CloudServiceType {
    /**
     * Customer Equipment Gateway
     */
    CEGW(1),

    /**
     * CNA Gateway
     */
    CNAGW(2),

    /**
     * Unsupported enum value
     */
    UNSUPPORTED(-1);
    /**
     * Identifier value
     */
    private final long id;
    private final static Map<Long, CloudServiceType> ID_MAP;

    static {
        CloudServiceType[] values = CloudServiceType.values();
        ID_MAP = new HashMap<>(values.length);

        for (CloudServiceType v : values) {
            ID_MAP.put(v.getId(), v);
        }
    }

    CloudServiceType(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static CloudServiceType getById(long id) {
        return ID_MAP.get(id);
    }

    @JsonCreator
    public static CloudServiceType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, CloudServiceType.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(CloudServiceType value) {
        return UNSUPPORTED.equals(value);
    }
}
