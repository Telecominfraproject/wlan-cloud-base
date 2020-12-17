package com.telecominfraproject.wlan.core.model.equipment.vendorextensions;

import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class VendorExtendedEquipmentTypeModel extends BaseJsonModel{

    private static final long serialVersionUID = -7571240065645860754L;
    
    private String name;
    private EquipmentType dataType;
    
    protected VendorExtendedEquipmentTypeModel() {
        //for deserializer
    }
    
    public VendorExtendedEquipmentTypeModel(String name, EquipmentType dataType) {
        this.name = name;
        this.dataType = dataType;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public EquipmentType getDataType() {
        return dataType;
    }
    public void setDataType(EquipmentType dataType) {
        this.dataType = dataType;
    }

}
