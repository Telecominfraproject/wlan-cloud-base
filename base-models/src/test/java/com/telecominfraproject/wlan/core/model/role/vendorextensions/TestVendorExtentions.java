package com.telecominfraproject.wlan.core.model.role.vendorextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.role.PortalUserRole;

public class TestVendorExtentions {

    @Test
    public void testExtendedEnum() {
        VendorExtendedPortalUserRoleModel t1 = new VendorExtendedPortalUserRoleModel("t1", PortalUserRole.CustomerIT);
        VendorExtendedPortalUserRoleModel t2 = new VendorExtendedPortalUserRoleModel("t2", VendorExtendedPortalUserRole.CustomerIT);
        VendorExtendedPortalUserRoleModel t3 = new VendorExtendedPortalUserRoleModel("t3", VendorExtendedPortalUserRole.VENDOR_USER_ROLE_B);
        
        VendorExtendedPortalUserRoleModel t1d = BaseJsonModel.fromString(t1.toString(), VendorExtendedPortalUserRoleModel.class);  
        VendorExtendedPortalUserRoleModel t2d = BaseJsonModel.fromString(t2.toString(), VendorExtendedPortalUserRoleModel.class);  
        VendorExtendedPortalUserRoleModel t3d = BaseJsonModel.fromString(t3.toString(), VendorExtendedPortalUserRoleModel.class);

        assertEquals(t1.toString(), t1d.toString());
        assertEquals(t2.toString(), t2d.toString());
        assertEquals(t3.toString(), t3d.toString());
        
    }

}
