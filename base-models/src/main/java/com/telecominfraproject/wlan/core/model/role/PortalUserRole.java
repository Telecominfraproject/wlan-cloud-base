/**
 * 
 */
package com.telecominfraproject.wlan.core.model.role;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

/**
 * @author ekeddy
 *
 *         Defines the various user roles and permissions. Highest permission
 *         level has greatest permission.
 */
public enum PortalUserRole {
    /**
     * Public user - Read Only
     */
    Public(0, 0),
    /**
     * Customer - Read Write
     */
    CustomerIT(1, 10),
    /**
     * MSP - Read Write
     */
    ManagedServiceProvider(2, 20),
    /**
     * Service Distributor - Read Write
     */
    Distributor(3, 30),
    /**
     * Technical Support - Read Write
     */
    TechSupport(4, 40),
    /**
     * Super User - Read Write
     */
    SuperUser(Integer.MAX_VALUE, Integer.MAX_VALUE),
    /**
     * Customer - Read Only
     */
    CustomerIT_RO(5, 5),
    /**
     * MSP - Read Only
     */
    ManagedServiceProvider_RO(6, 15),
    /**
     * Service Distributor - Read Only
     */
    Distributor_RO(7, 25),
    /**
     * Technical Support - Read Only
     */
    TechSupport_RO(8, 35),
    /**
     * Super User - Read Only
     */
    SuperUser_RO(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 5),    
    /**
     * Unknown
     */
    Unknown(-1, -1);

    private final static Map<Integer, PortalUserRole> ID_MAP;
    static {
        ID_MAP = new HashMap<>();
        for (PortalUserRole r : PortalUserRole.values()) {
            if (PortalUserRole.isUnsupported(r)) {
                continue;
            }
            ID_MAP.put(r.getId(), r);
        }
    }

    public static final String SYSTEM_IDENTIFIER = "System";

    public static PortalUserRole getById(int id) {
        PortalUserRole result = ID_MAP.get(id);
        if (result == null) {
            return PortalUserRole.Unknown;
        }
        return result;
    }

    @JsonCreator
    public static PortalUserRole getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, PortalUserRole.class, Unknown);
    }

    public static boolean isUnsupported(PortalUserRole userRole) {
        return Unknown.equals(userRole);
    }

    private final int permissionLevel;

    private final int id;

    private PortalUserRole(int id, int permissionLevel) {
        this.id = id;
        this.permissionLevel = permissionLevel;
    }

    public int getId() {
        return id;
    }

    public int getPermissionLevel() {
        return this.permissionLevel;
    }
}
