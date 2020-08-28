package com.telecominfraproject.wlan.core.model.extensibleenum.example;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * Example of a usage of vendor-specific extensions to the original enum
 * @author dtop
 *
 */
public class VendorExtendedEnumExampleModel extends BaseJsonModel{

    private static final long serialVersionUID = 3971454239421363299L;
    
    private String name;
    private OriginalEnumExample dataType;
    
    @SuppressWarnings("unused")
    private VendorExtendedEnumExampleModel() {
        //for deserializer
    }
    
    public VendorExtendedEnumExampleModel(String name, OriginalEnumExample dataType) {
        this.name = name;
        this.dataType = dataType;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public OriginalEnumExample getDataType() {
        return dataType;
    }
    public void setDataType(OriginalEnumExample dataType) {
        this.dataType = dataType;
    }

    public static void main(String[] args) {
        VendorExtendedEnumExampleModel t1 = new VendorExtendedEnumExampleModel("t1", OriginalEnumExample.VALUE_A);
        VendorExtendedEnumExampleModel t2 = new VendorExtendedEnumExampleModel("t2", ExtendedEnumExample.VALUE_A);
        VendorExtendedEnumExampleModel t3 = new VendorExtendedEnumExampleModel("t3", ExtendedEnumExample.VENDOR_VALUE_A);
        
        System.out.println("t1  = "+ t1);
        System.out.println("t2  = "+ t2);
        System.out.println("t3  = "+ t3);
        
        VendorExtendedEnumExampleModel t1d = BaseJsonModel.fromString(t1.toString(), VendorExtendedEnumExampleModel.class);  
        VendorExtendedEnumExampleModel t2d = BaseJsonModel.fromString(t2.toString(), VendorExtendedEnumExampleModel.class);  
        VendorExtendedEnumExampleModel t3d = BaseJsonModel.fromString(t3.toString(), VendorExtendedEnumExampleModel.class);

        System.out.println("=======================");
        
        System.out.println("t1d = "+ t1d);
        System.out.println("t2d = "+ t2d);
        System.out.println("t3d = "+ t3d);

        System.out.println("=======================");

        System.out.println("OriginalEnumExample.VALUE_A = "+ OriginalEnumExample.VALUE_A);
        System.out.println("ExtendedEnumExample.VALUE_A = "+ ExtendedEnumExample.VALUE_A);
        System.out.println("ExtendedEnumExample.VENDOR_VALUE_A = "+ ExtendedEnumExample.VENDOR_VALUE_A);

    }

}
