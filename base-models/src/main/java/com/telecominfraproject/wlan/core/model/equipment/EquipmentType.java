package com.telecominfraproject.wlan.core.model.equipment;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum EquipmentType {

    AP            (1),
    MG            (2),
    SWITCH        (3),
    CUSTOMER_NETWORK_AGENT (4),
    UNSUPPORTED   (-1);
    
    private final int id;
    private static final Map<Integer, EquipmentType> ELEMENTS = new HashMap<>();
    
    private EquipmentType(int id){
        this.id = id;
    }
    
    public int getId(){
        return this.id;
    }
    
    public static EquipmentType getById(int enumId){
        if(ELEMENTS.isEmpty()){
            //initialize elements map
            for(EquipmentType met : EquipmentType.values()){
                ELEMENTS.put(met.getId(), met);
            }
        }
        
        return ELEMENTS.get(enumId);
    }
    
    @JsonCreator
    public static EquipmentType getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, EquipmentType.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(EquipmentType value) {
        return (UNSUPPORTED.equals(value));
    }
}
