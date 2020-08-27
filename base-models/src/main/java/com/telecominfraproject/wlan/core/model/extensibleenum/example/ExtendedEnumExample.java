package com.telecominfraproject.wlan.core.model.extensibleenum.example;

/**
 * Example of how to define a vendor-specific extensions to the original enum
 *
 * @author dtop
 *
 */
public class ExtendedEnumExample extends OriginalEnumExample {
        
    public static final OriginalEnumExample 
    VENDOR_VALUE_A = new ExtendedEnumExample(100, "VENDOR_VALUE_A") ,
    VENDOR_VALUE_B = new ExtendedEnumExample(101, "VENDOR_VALUE_B")
        ;
    
    private ExtendedEnumExample(int id, String name) {
        super(id, name);
    }

 }
