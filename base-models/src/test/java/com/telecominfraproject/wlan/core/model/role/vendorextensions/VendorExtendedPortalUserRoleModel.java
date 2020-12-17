package com.telecominfraproject.wlan.core.model.role.vendorextensions;

import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.role.PortalUserRole;

public class VendorExtendedPortalUserRoleModel extends BaseJsonModel{

    private static final long serialVersionUID = -7571240065645860754L;
    
    private String name;
    private PortalUserRole role;
    
    protected VendorExtendedPortalUserRoleModel() {
        //for deserializer
    }
    
    public VendorExtendedPortalUserRoleModel(String name, PortalUserRole role) {
        this.name = name;
        this.role = role;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public PortalUserRole getRole() {
        return role;
    }
    public void setRole(PortalUserRole role) {
        this.role = role;
    }

}
