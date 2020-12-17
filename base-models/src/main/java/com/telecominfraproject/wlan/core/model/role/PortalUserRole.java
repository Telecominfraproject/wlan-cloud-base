/**
 * 
 */
package com.telecominfraproject.wlan.core.model.role;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * All available roles for the portal users that can be handled by the CloudSDK. 
 * <br>This enumeration-like class can be extended by vendors - new elements can be defined by extending this class like so:
 * <br>
 * <pre>
 * public class VendorExtendedPortalUserRole extends PortalUserRole {
 *    
 *    public static final PortalUserRole 
 *    VENDOR_USER_ROLE_A = new VendorExtendedPortalUserRole(100, "VENDOR_USER_ROLE_A", 5) ,
 *    VENDOR_USER_ROLE_B = new VendorExtendedPortalUserRole(101, "VENDOR_USER_ROLE_B", 5)
 *    ;
 *
 *    private VendorExtendedPortalUserRole(int id, String name, int permissionLevel) {
 *        super(id, name, permissionLevel);
 *    }
 *
 * }
 * </pre>
 * @see com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId
 * @see com.telecominfraproject.wlan.core.model.role.vendorextensions.VendorExtendedPortalUserRole
 * <br>
 * @author dtop
 * @author ekeddy
 *
 *         Defines the various user roles and permissions. Highest permission
 *         level has greatest permission.
 */
public class PortalUserRole implements EnumWithId {
    
    private static final Logger LOG = LoggerFactory.getLogger(PortalUserRole.class);

    private static Object lock = new Object();
    private static final Map<Integer, PortalUserRole> ELEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, PortalUserRole> ELEMENTS_BY_NAME = new ConcurrentHashMap<>();

    public static final PortalUserRole 

    /**
     * Public user - Read Only
     */
    Public = new PortalUserRole(0, "Public", 0),
    /**
     * Customer - Read Write
     */
    CustomerIT = new PortalUserRole(1, "CustomerIT", 10),
    /**
     * MSP - Read Write
     */
    ManagedServiceProvider = new PortalUserRole(2, "ManagedServiceProvider", 20),
    /**
     * Service Distributor - Read Write
     */
    Distributor = new PortalUserRole(3, "Distributor", 30),
    /**
     * Technical Support - Read Write
     */
    TechSupport = new PortalUserRole(4, "TechSupport", 40),
    /**
     * Super User - Read Write
     */
    SuperUser = new PortalUserRole(Integer.MAX_VALUE, "SuperUser", Integer.MAX_VALUE),
    /**
     * Customer - Read Only
     */
    CustomerIT_RO = new PortalUserRole(5, "CustomerIT_RO", 5),
    /**
     * MSP - Read Only
     */
    ManagedServiceProvider_RO = new PortalUserRole(6, "ManagedServiceProvider_RO", 15),
    /**
     * Service Distributor - Read Only
     */
    Distributor_RO = new PortalUserRole(7, "Distributor_RO", 25),
    /**
     * Technical Support - Read Only
     */
    TechSupport_RO = new PortalUserRole(8, "TechSupport_RO", 35),
    /**
     * Super User - Read Only
     */
    SuperUser_RO = new PortalUserRole(Integer.MAX_VALUE - 1, "SuperUser_RO", Integer.MAX_VALUE - 5),    
    /**
     * Unknown
     */
    Unknown = new PortalUserRole(-1, "Unknown", -1);

    static {
        //try to load all the subclasses explicitly - to avoid timing issues when items coming from subclasses may be registered some time later, after the parent class is loaded 
        Set<Class<? extends PortalUserRole>> subclasses = BaseJsonModel.getReflections().getSubTypesOf(PortalUserRole.class);
        for(Class<?> cls: subclasses) {
            try {
                Class.forName(cls.getName());
            } catch (ClassNotFoundException e) {
                LOG.warn("Cannot load class {} : {}", cls.getName(), e);
            }
        }
    }  


    public static final String SYSTEM_IDENTIFIER = "System";

    private final int permissionLevel;

    private final int id;
    private final String name;
    

    protected PortalUserRole(int id, String name, int permissionLevel) {
        synchronized(lock) {
            
            LOG.debug("Registering PortalUserRole by {} : {}", this.getClass().getSimpleName(), name);

            this.id = id;
            this.name = name;
            this.permissionLevel = permissionLevel;

            ELEMENTS_BY_NAME.values().forEach(s -> {
                if(s.getName().equals(name)) {
                    throw new IllegalStateException("PortalUserRole item for "+ name + " is already defined, cannot have more than one of them");
                }                
            });
    
            if(ELEMENTS.containsKey(id)) {
                throw new IllegalStateException("PortalUserRole item "+ name + "("+id+") is already defined, cannot have more than one of them");
            }
    
            ELEMENTS.put(id, this);
            ELEMENTS_BY_NAME.put(name, this);
        }
    }

    public int getId() {
        return id;
    }

    public int getPermissionLevel() {
        return this.permissionLevel;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PortalUserRole)) {
            return false;
        }
        PortalUserRole other = (PortalUserRole) obj;
        return id == other.id;
    }   

    public String toString() {
        return name;
    }
    
    @JsonIgnore
    public static PortalUserRole[] values() {
        return new ArrayList<>(ELEMENTS.values()).toArray(new PortalUserRole[0]);
    }

    public static PortalUserRole getById(int enumId){

        PortalUserRole result = ELEMENTS.get(enumId);
        if (result == null) {
            return Unknown;
        }
        return result;
        
    }
    
    @JsonCreator
    public static PortalUserRole getByName(String value) {
        PortalUserRole ret = ELEMENTS_BY_NAME.get(value);
        if (ret == null) {
            ret = Unknown;
        }
        
        return ret;
    }


    public static List<PortalUserRole> getValues() {
        return new ArrayList<>(ELEMENTS.values());
    }
    
    public static boolean isUnsupported(PortalUserRole value) {
        return (Unknown.equals(value));
    }


}
