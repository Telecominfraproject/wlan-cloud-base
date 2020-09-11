package com.telecominfraproject.wlan.core.model.equipment.vendorextensions;

import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;

public class VendorExtendedEquipmentType extends EquipmentType {
    
    public static final EquipmentType 
    VENDOR_EQ_A = new VendorExtendedEquipmentType(100, "VENDOR_EQ_A") ,
    VENDOR_EQ_B = new VendorExtendedEquipmentType(101, "VENDOR_EQ_B")
    ;

    private VendorExtendedEquipmentType(int id, String name) {
        super(id, name);
    }

}
