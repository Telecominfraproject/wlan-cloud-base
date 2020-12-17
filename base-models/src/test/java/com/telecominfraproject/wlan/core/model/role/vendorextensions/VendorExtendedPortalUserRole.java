package com.telecominfraproject.wlan.core.model.role.vendorextensions;

import com.telecominfraproject.wlan.core.model.role.PortalUserRole;

public class VendorExtendedPortalUserRole extends PortalUserRole {
    
    public static final PortalUserRole 
    VENDOR_USER_ROLE_A = new VendorExtendedPortalUserRole(100, "VENDOR_USER_ROLE_A", 5) ,
    VENDOR_USER_ROLE_B = new VendorExtendedPortalUserRole(101, "VENDOR_USER_ROLE_B", 5)
    ;

    private VendorExtendedPortalUserRole(int id, String name, int permissionLevel) {
        super(id, name, permissionLevel);
    }

 }