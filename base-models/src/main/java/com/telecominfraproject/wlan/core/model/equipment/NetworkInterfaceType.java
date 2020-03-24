/**
 * 
 */
package com.telecominfraproject.wlan.core.model.equipment;

import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.NetworkInterfaceType;

/**
 * Different type of interface based on MAC address allocation.
 * 
 * @author yongli
 *
 */
public enum NetworkInterfaceType {

    /**
     * Ethernet Interface, MAC ends with [Even]F.
     */
    ETHERNET(0),
    /**
     * 2.4G RADIO, MAC ends with [Even][0-D]
     */
    RADIO_2G(1),
    /**
     * 5G RADIO, MAC ends with [Odd][0-D]
     */
    RADIO_5G(2),
    /**
     * Reserved MAC, un-used
     */
    RESERVED(3),
    /**
     * Invalid, not supported
     */
    UNSUPPORTED(-1);

    private final int id;

    private NetworkInterfaceType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Get the interface type based on the MAC address
     * 
     * @param macAddress
     * @return interface type
     */
    public static NetworkInterfaceType interfaceType(final MacAddress macAddress) {
        if (!isArt2waveMAC(macAddress)) {
            return UNSUPPORTED;
        }
        byte[] rawAddress = macAddress.getAddress();
        int lastByte = rawAddress[rawAddress.length - 1];
        if (0 == (lastByte >> 4) % 2) {
            switch (lastByte & 0x0F) {
            case 0x0E:
                return RESERVED;
            case 0x0F:
                return ETHERNET;
            default:
                return RADIO_2G;
            }
        } else {
            switch (lastByte & 0x0F) {
            case 0x0E:
            case 0x0F:
                return RESERVED;
            default:
                return RADIO_5G;
            }
        }

    }

    /**
     * Test if a MAC address is art2wave equipment.
     * 
     * ART2WAVE_MAC_PREFIX "0x749ce3";
     * 
     * @param macAddress
     * @return
     */
    public static boolean isArt2waveMAC(final MacAddress macAddress) {
        if (null == macAddress) {
            return false;
        }
        return isArt2waveMAC(macAddress.getAddress());
    }

    /**
     * Test if a MAC address is art2wave equipment.
     * 
     * ART2WAVE_MAC_PREFIX "0x749ce3";
     * 
     * @param macAddress
     * @return
     */
    public static boolean isArt2waveMAC(final byte[] rawAddress) {
        if (null == rawAddress) {
            return false;
        }
        if (rawAddress.length != MacAddress.VALUE_LENGTH) {
            return false;
        }
        if ((byte) 0x74 != rawAddress[0] || (byte) 0x9c != rawAddress[1] || (byte) 0xe3 != rawAddress[2]) {
            return false;
        }
        return true;
    }

    /**
     * Calculate Staring MAC Address for a specific interface
     * 
     * @param baseAddress
     * @param interfaceType
     * @return
     */
    public static MacAddress getInterfaceMac(final MacAddress baseAddress, NetworkInterfaceType interfaceType) {
        if (!isArt2waveMAC(baseAddress)) {
            return null;
        }
        Long rawAddress = baseAddress.getAddressAsLong();

        switch (interfaceType) {
        case RADIO_2G:
            break;
        case RADIO_5G:
            rawAddress += 0x10;
            break;
        case ETHERNET:
            rawAddress += 0x0F;
            break;
        default:
            rawAddress = null;
        }
        if (null == rawAddress) {
            return null;
        }
        return MacAddress.valueOf(rawAddress);

    }
}
