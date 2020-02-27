package com.whizcontrol.core.model.equipment;

import com.whizcontrol.core.model.json.BaseJsonModel;

public class MacToMonitor extends BaseJsonModel {
    private static final long serialVersionUID = 6944693708625261094L;
    private MacAddress mac;
    private int maskBits; // //bits for masking mac address matching, e.g.:
                          // maskBits=8, 08:5b:0e:7d:16:xx

    public MacToMonitor() {
        // serial
    }

    public MacToMonitor(MacAddress mac, int maskBits) {
        super();
        this.mac = mac;
        this.maskBits = maskBits;
    }

    public MacAddress getMac() {
        return mac;
    }

    public void setMac(MacAddress mac) {
        this.mac = mac;
    }

    public int getMaskBits() {
        return maskBits;
    }

    public void setMaskBits(int maskBits) {
        this.maskBits = maskBits;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mac == null) ? 0 : mac.hashCode());
        result = prime * result + maskBits;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MacToMonitor other = (MacToMonitor) obj;
        if (mac == null) {
            if (other.mac != null)
                return false;
        } else if (!mac.equals(other.mac))
            return false;
        if (maskBits != other.maskBits)
            return false;
        return true;
    }

    @Override
    public boolean hasUnsupportedValue() {
        return false;
    }

}
