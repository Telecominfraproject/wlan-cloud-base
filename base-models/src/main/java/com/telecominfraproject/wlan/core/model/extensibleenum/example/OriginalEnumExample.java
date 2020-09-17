package com.telecominfraproject.wlan.core.model.extensibleenum.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * Example of Enumeration-like class that can be extended by vendors - new elements can be defined by extending this class.
 * <br>@see com.telecominfraproject.wlan.core.model.extensibleenum.example.ExtendedEnumExample
 * <br>@see com.telecominfraproject.wlan.core.model.extensibleenum.example.VendorExtendedEnumExampleModel
 * <br>
 * @author dtop
 *
 */
public class OriginalEnumExample implements EnumWithId {
    
    private static final Logger LOG = LoggerFactory.getLogger(OriginalEnumExample.class);

    private static Object lock = new Object();
    private static final Map<Integer, OriginalEnumExample> ELEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, OriginalEnumExample> ELEMENTS_BY_NAME = new ConcurrentHashMap<>();

    public static final OriginalEnumExample 
    VALUE_A = new OriginalEnumExample(1, "VALUE_A") ,
    VALUE_B = new OriginalEnumExample(2, "VALUE_B") ,

    UNSUPPORTED  = new OriginalEnumExample(-1, "UNSUPPORTED") ;
    
    static {
        //try to load all the subclasses explicitly - to avoid timing issues when items coming from subclasses may be registered some time later, after the parent class is loaded 
        Set<Class<? extends OriginalEnumExample>> subclasses = BaseJsonModel.getReflections().getSubTypesOf(OriginalEnumExample.class);
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
    
    protected OriginalEnumExample(int id, String name) {
        synchronized(lock) {
            
            LOG.debug("Registering OriginalEnumExample by {} : {}", this.getClass().getSimpleName(), name);

            this.id = id;
            this.name = name;

            ELEMENTS_BY_NAME.values().forEach(s -> {
                if(s.getName().equals(name)) {
                    throw new IllegalStateException("OriginalEnumExample item for "+ name + " is already defined, cannot have more than one of them");
                }                
            });
    
            if(ELEMENTS.containsKey(id)) {
                throw new IllegalStateException("OriginalEnumExample item "+ name + "("+id+") is already defined, cannot have more than one of them");
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

    public static OriginalEnumExample getById(int enumId){
        return ELEMENTS.get(enumId);
    }
    
    @JsonCreator
    public static OriginalEnumExample getByName(String value) {
        OriginalEnumExample ret = ELEMENTS_BY_NAME.get(value);
        if (ret == null) {
            ret = UNSUPPORTED;
        }
        
        return ret;
    }


    public static List<OriginalEnumExample> getValues() {
        return new ArrayList<>(ELEMENTS.values());
    }
    
    public static boolean isUnsupported(OriginalEnumExample value) {
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
        if (!(obj instanceof OriginalEnumExample)) {
            return false;
        }
        OriginalEnumExample other = (OriginalEnumExample) obj;
        return id == other.id;
    }   

    @Override
    public String toString() {
        return name;
    }

}
