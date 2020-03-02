/**
 * 
 */
package com.telecominfraproject.wlan.core.model.equipment;

/**
 * @author yongli
 *
 */
public final class NetworkConstants {
    /**
     * Minimum value for VLAN ID
     */
    static public final short VLAN_MIN_VALUE = 0;
    /**
     * Maximum value for VLAN ID
     */
    static public final short VLAN_MAX_VALUE = 4095;
    /**
     * Minimum value can be provisioned
     */
    static public final short VLAN_MIN_PROVISION = 2;
    /**
     * Maximum value can be provisioned
     */
    static public final short VLAN_MAX_PROVIONS = 4094;
    /**
     * Default VLAN value
     */
    static public final short VLAN_DEFAULT_VALUE = 1;

    /**
     * Hide constructor
     */
    private NetworkConstants() {
    }

    /**
     * Check if VLAN tag can be provisioned
     * 
     * @param vlanTag
     * @return true if value can be provisioned.
     */
    public static boolean isValidVLANTagForProvision(Short vlanTag) {
        if (null == vlanTag) {
            return false;
        }
        if ((vlanTag < VLAN_MIN_VALUE || vlanTag > VLAN_MAX_VALUE)) {
            return false;
        }
        return true;
    }
}
