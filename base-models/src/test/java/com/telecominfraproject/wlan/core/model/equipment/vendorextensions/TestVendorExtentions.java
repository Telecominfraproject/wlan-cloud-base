package com.telecominfraproject.wlan.core.model.equipment.vendorextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class TestVendorExtentions {

    @Test
    public void testExtendedEnum() {
        VendorExtendedEquipmentTypeModel t1 = new VendorExtendedEquipmentTypeModel("t1", EquipmentType.AP);
        VendorExtendedEquipmentTypeModel t2 = new VendorExtendedEquipmentTypeModel("t2", VendorExtendedEquipmentType.AP);
        VendorExtendedEquipmentTypeModel t3 = new VendorExtendedEquipmentTypeModel("t3", VendorExtendedEquipmentType.VENDOR_EQ_B);

        VendorExtendedEquipmentTypeModel t1d = BaseJsonModel.fromString(t1.toString(), VendorExtendedEquipmentTypeModel.class);  
        VendorExtendedEquipmentTypeModel t2d = BaseJsonModel.fromString(t2.toString(), VendorExtendedEquipmentTypeModel.class);  
        VendorExtendedEquipmentTypeModel t3d = BaseJsonModel.fromString(t3.toString(), VendorExtendedEquipmentTypeModel.class);

        assertEquals(t1.toString(), t1d.toString());
        assertEquals(t2.toString(), t2d.toString());
        assertEquals(t3.toString(), t3d.toString());
        
    }

}
