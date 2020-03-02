package com.telecominfraproject.wlan.core.model.equipment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;


public enum RadioType {

    is5GHz(0),
    is2dot4GHz(1),
    UNSUPPORTED(-1);
    
    private final int id;
    private static final Map<Integer, RadioType> ELEMENTS = new HashMap<>();
    private static final RadioType validValues[] = { is5GHz, is2dot4GHz };
    
    private RadioType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static RadioType getById(int enumId) {
        if (ELEMENTS.isEmpty()) {
            synchronized (ELEMENTS) {
                if (ELEMENTS.isEmpty()) {
                    //initialize elements map
                    for(RadioType met : RadioType.values()) {
                        ELEMENTS.put(met.getId(), met);
                    }
                }
            }
        }
        return ELEMENTS.get(enumId);
    }

    public static RadioType[] validValues()
    {
        return validValues;
    }
    
    @JsonCreator
    public static RadioType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, RadioType.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(RadioType radioType) {
        return (UNSUPPORTED.equals(radioType));
    }
    
    public static boolean hasUnsupportedValue(Collection<RadioType> radioTypes) 
    {
        if(radioTypes != null)
        {
            for(RadioType type : radioTypes)
            {
                if(isUnsupported(type))
                {
                    return true;
                }
            }
        }
        
        return false;
    }

    
}
