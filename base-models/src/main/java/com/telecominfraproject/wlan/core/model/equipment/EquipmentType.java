package com.telecominfraproject.wlan.core.model.equipment;

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
 * All available EquipmentTypes that can be handled by the CloudSDK. 
 * <br>This enumeration-like class can be extended by vendors - new elements can be defined by extending this class like so:
 * <br>
 * <pre>
 * public class VendorExtendedEquipmentType extends EquipmentType {
 *    
 *    public static final EquipmentType 
 *    VENDOR_EQ_A = new VendorExtendedEquipmentType(100, "VENDOR_EQ_A") ,
 *    VENDOR_EQ_B = new VendorExtendedEquipmentType(101, "VENDOR_EQ_B")
 *    ;
 *
 *    private VendorExtendedEquipmentType(int id, String name) {
 *        super(id, name);
 *    }
 *
 * }
 * </pre>
 * @see com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId
 * @see com.telecominfraproject.wlan.core.model.equipment.vendorextensions.TestVendorExtendedEquipmentTypeModel
 * <br>
 * @author dtop
 *
 */
public class EquipmentType implements EnumWithId {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentType.class);

    private static Object lock = new Object();
    private static final Map<Integer, EquipmentType> ELEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, EquipmentType> ELEMENTS_BY_NAME = new ConcurrentHashMap<>();

    public static final EquipmentType 

    AP = new EquipmentType(1, "AP"),
    MG = new EquipmentType(2, "MG"),
    SWITCH = new EquipmentType(3, "SWITCH"),
    CUSTOMER_NETWORK_AGENT  = new EquipmentType(4, "CUSTOMER_NETWORK_AGENT"),
    UNSUPPORTED = new EquipmentType(-1, "UNSUPPORTED");
    
    static {
        //try to load all the subclasses explicitly - to avoid timing issues when items coming from subclasses may be registered some time later, after the parent class is loaded 
        Set<Class<? extends EquipmentType>> subclasses = BaseJsonModel.getReflections().getSubTypesOf(EquipmentType.class);
        for(Class<?> cls: subclasses) {
            try {
                Class.forName(cls.getName());
            } catch (ClassNotFoundException e) {
                LOG.warn("Cannot load class {} : {}", cls.getName(), e);
            }
        }
    }  

    private final int id;
    private final String name;
    
    protected EquipmentType(int id, String name) {
        synchronized(lock) {

            LOG.debug("Registering EquipmentType by {} : {}", this.getClass().getSimpleName(), name);

            this.id = id;
            this.name = name;

            ELEMENTS_BY_NAME.values().forEach(s -> {
                if(s.getName().equals(name)) {
                    throw new IllegalStateException("EquipmentType item for "+ name + " is already defined, cannot have more than one of them");
                }                
            });
    
            if(ELEMENTS.containsKey(id)) {
                throw new IllegalStateException("EquipmentType item "+ name + "("+id+") is already defined, cannot have more than one of them");
            }
    
            ELEMENTS.put(id, this);
            ELEMENTS_BY_NAME.put(name, this);
        }
    }
    
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @JsonIgnore
    public String name() {
        return name;
    }

    @JsonIgnore
    public static EquipmentType[] values() {
        return new ArrayList<>(ELEMENTS.values()).toArray(new EquipmentType[0]);
    }

    public static EquipmentType getById(int enumId){
        return ELEMENTS.get(enumId);
    }
    
    @JsonCreator
    public static EquipmentType getByName(String value) {
        EquipmentType ret = ELEMENTS_BY_NAME.get(value);
        if (ret == null) {
            ret = UNSUPPORTED;
        }
        
        return ret;
    }

    public static List<EquipmentType> getValues() {
        return new ArrayList<>(ELEMENTS.values());
    }
    
    public static boolean isUnsupported(EquipmentType value) {
        return (UNSUPPORTED.equals(value));
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
        if (!(obj instanceof EquipmentType)) {
            return false;
        }
        EquipmentType other = (EquipmentType) obj;
        return id == other.id;
    }   

    @Override
    public String toString() {
        return name;
    }
    

}
