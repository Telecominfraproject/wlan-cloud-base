/**
 * 
 */
package com.telecominfraproject.wlan.core.model.service;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;
import com.telecominfraproject.wlan.core.model.service.GatewayType;

/**
 * Different type of Cloud Service
 * 
 * @author yongli
 *
 */
public enum GatewayType {
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
    private final int id;
    private final static Map<Integer, GatewayType> ID_MAP;

    static {
        GatewayType[] values = GatewayType.values();
        ID_MAP = new HashMap<>(values.length);

        for (GatewayType v : values) {
            ID_MAP.put(v.getId(), v);
        }
    }

    GatewayType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static GatewayType getById(int id) {
        return ID_MAP.get(id);
    }

    @JsonCreator
    public static GatewayType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, GatewayType.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(GatewayType value) {
        return UNSUPPORTED.equals(value);
    }
}
