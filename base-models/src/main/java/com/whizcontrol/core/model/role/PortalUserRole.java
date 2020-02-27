/**
 * 
 */
package com.whizcontrol.core.model.role;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * @author ekeddy
 *
 *         Defines the various user roles and permissions. Highest permission
 *         level has greatest permission.
 */
public enum PortalUserRole {
    /**
     * Public user
     */
    Public(0, 0),
    /**
     * Customer
     */
    CustomerIT(1, 10),
    /**
     * MSP
     */
    ManagedServiceProvider(2, 20),
    /**
     * Service Distributor
     */
    Distributor(3, 30),
    /**
     * Technical Support
     */
    TechSupport(4, 40),
    /**
     * Super User
     */
    SuperUser(Integer.MAX_VALUE, Integer.MAX_VALUE),
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
