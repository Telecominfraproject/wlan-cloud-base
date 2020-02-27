/**
 * 
 */
package com.whizcontrol.core.model.event;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * @author yongli
 *
 */
public enum RawEventType {
    /**
     * DHCP
     */
    DHCP(1),
    /**
     * 802.11 Association Request
     */
    WIFI_ASSOC_REQ(2),
    /**
     * 802.11 Association Response
     */
    WIFI_ASSOC_RESP(3),
    /**
     * 802.11 Re-association request
     */
    WIFI_REASSOC_REQ(4),
    /**
     * 802.11 Re-association response
     */
    WIFI_REASSOC_RESP(5),
    /**
     * 802.11 Probe request
     */
    WIFI_PROBE_REQ(6),
    /**
     * 802.11 Dis-assocation
     */
    WIFI_DISASSOC(7),
    /**
     * 802.11 authentication
     */
    WIFI_AUTH(8),
    /**
     * 802.11 de-authentication
     */
    WIFI_DEAUTH(9),
    /**
     * 802.11 action
     */
    WIFI_ACTION(10),
    /**
     * 802.11 action no ack
     */
    WIFI_ACTION_NOACK(11),
    /**
     * Probe for foreign network SSID.
     * 
     * Event should contain the client MAC in the descriptor. RAW packet body is
     * not included.
     */
    WIFI_FN_PROBE_REQ(12),
    /**
     * De-Authentication attack
     */
    WIFI_DEAUTH_ATTACK(106),
    
    // MAXMUM value allowed is 999 due to the DB encoding offset
    UNSUPPORTED(999);
    
    /**
     * Identifier for the enum
     */
    private final int id;

    private static final Map<Integer, RawEventType> ELEMENTS = new HashMap<>();

    static {
        for (RawEventType v : RawEventType.values()) {
            ELEMENTS.put(v.getId(), v);
        }
    }

    /**
     * Constructor
     * 
     * @param id
     */
    RawEventType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Get enum by identifier
     * 
     * @param enumId
     * @return decoded
     */
    public static RawEventType getById(int enumId) {
        return (ELEMENTS.get(enumId));
    }
    
    @JsonCreator
    public static RawEventType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, RawEventType.class, UNSUPPORTED);
    }
    
    public static boolean isUnsupported(RawEventType value) {
        return UNSUPPORTED.equals(value);
    }
}
