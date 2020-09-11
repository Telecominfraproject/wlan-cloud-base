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

    public static void main(String[] args) {
        VendorExtendedEquipmentTypeModel t1 = new VendorExtendedEquipmentTypeModel("t1", EquipmentType.AP);
        VendorExtendedEquipmentTypeModel t2 = new VendorExtendedEquipmentTypeModel("t2", VendorExtendedEquipmentType.AP);
        VendorExtendedEquipmentTypeModel t3 = new VendorExtendedEquipmentTypeModel("t3", VendorExtendedEquipmentType.VENDOR_EQ_B);
        
        System.out.println("t1  = "+ t1);
        System.out.println("t2  = "+ t2);
        System.out.println("t3  = "+ t3);
        
        VendorExtendedEquipmentTypeModel t1d = BaseJsonModel.fromString(t1.toString(), VendorExtendedEquipmentTypeModel.class);  
        VendorExtendedEquipmentTypeModel t2d = BaseJsonModel.fromString(t2.toString(), VendorExtendedEquipmentTypeModel.class);  
        VendorExtendedEquipmentTypeModel t3d = BaseJsonModel.fromString(t3.toString(), VendorExtendedEquipmentTypeModel.class);

        System.out.println("=======================");
        
        System.out.println("t1d = "+ t1d);
        System.out.println("t2d = "+ t2d);
        System.out.println("t3d = "+ t3d);

    }
}
