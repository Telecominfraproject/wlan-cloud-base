 package com.telecominfraproject.wlan.core.model.equipment;

import java.util.concurrent.TimeUnit;

import com.telecominfraproject.wlan.core.model.equipment.ChannelHopSettings;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class ChannelHopSettings extends BaseJsonModel
{
    /**
     * 
     */
    private static final long serialVersionUID = 2026234712530816617L;
    private Integer noiseFloorThresholdInDB;
    private Integer noiseFloorThresholdTimeInSeconds;
    private Integer nonWifiThresholdInPercentage;
    private Integer nonWifiThresholdTimeInSeconds;
    private OBSSHopMode obssHopMode;

    /*
     * Defaults described here:
     * https://kdc.atlassian.net/browse/NAAS-4886
     */
    private ChannelHopSettings()
    {
        // serial
        this.noiseFloorThresholdInDB = -75;
        this.noiseFloorThresholdTimeInSeconds = (int) TimeUnit.MINUTES.toSeconds(3);
        this.nonWifiThresholdInPercentage = 50;
        this.nonWifiThresholdTimeInSeconds = (int) TimeUnit.MINUTES.toSeconds(3);
        this.obssHopMode = OBSSHopMode.NON_WIFI;
    }

    public Integer getNoiseFloorThresholdInDB() {
        return noiseFloorThresholdInDB;
    }

    public void setNoiseFloorThresholdInDB(Integer noiseFloorThresholdInDB) {
        this.noiseFloorThresholdInDB = noiseFloorThresholdInDB;
    }

    public Integer getNoiseFloorThresholdTimeInSeconds() {
        return noiseFloorThresholdTimeInSeconds;
    }

    public void setNoiseFloorThresholdTimeInSeconds(Integer noiseFloorThresholdTimeInSeconds) {
        this.noiseFloorThresholdTimeInSeconds = noiseFloorThresholdTimeInSeconds;
    }

    public Integer getNonWifiThresholdInPercentage() {
        return nonWifiThresholdInPercentage;
    }

    public void setNonWifiThresholdInPercentage(Integer nonWifiThresholdInPercentage) {
        this.nonWifiThresholdInPercentage = nonWifiThresholdInPercentage;
    }

    public Integer getNonWifiThresholdTimeInSeconds() {
        return nonWifiThresholdTimeInSeconds;
    }

    public void setNonWifiThresholdTimeInSeconds(Integer nonWifiThresholdTimeInSeconds) {
        this.nonWifiThresholdTimeInSeconds = nonWifiThresholdTimeInSeconds;
    }

    
    public OBSSHopMode getObssHopMode() {
        return obssHopMode;
    }

    public void setObssHopMode(OBSSHopMode obssHopMode) {
        this.obssHopMode = obssHopMode;
    }

    public static ChannelHopSettings createWithDefaults()
    {
        return new ChannelHopSettings();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((noiseFloorThresholdInDB == null) ? 0 : noiseFloorThresholdInDB.hashCode());
        result = prime * result
                + ((noiseFloorThresholdTimeInSeconds == null) ? 0 : noiseFloorThresholdTimeInSeconds.hashCode());
        result = prime * result
                + ((nonWifiThresholdInPercentage == null) ? 0 : nonWifiThresholdInPercentage.hashCode());
        result = prime * result
                + ((nonWifiThresholdTimeInSeconds == null) ? 0 : nonWifiThresholdTimeInSeconds.hashCode());
        result = prime * result + ((obssHopMode == null) ? 0 : obssHopMode.hashCode());
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
        ChannelHopSettings other = (ChannelHopSettings) obj;
        if (noiseFloorThresholdInDB == null) {
            if (other.noiseFloorThresholdInDB != null)
                return false;
        } else if (!noiseFloorThresholdInDB.equals(other.noiseFloorThresholdInDB))
            return false;
        if (noiseFloorThresholdTimeInSeconds == null) {
            if (other.noiseFloorThresholdTimeInSeconds != null)
                return false;
        } else if (!noiseFloorThresholdTimeInSeconds.equals(other.noiseFloorThresholdTimeInSeconds))
            return false;
        if (nonWifiThresholdInPercentage == null) {
            if (other.nonWifiThresholdInPercentage != null)
                return false;
        } else if (!nonWifiThresholdInPercentage.equals(other.nonWifiThresholdInPercentage))
            return false;
        if (nonWifiThresholdTimeInSeconds == null) {
            if (other.nonWifiThresholdTimeInSeconds != null)
                return false;
        } else if (!nonWifiThresholdTimeInSeconds.equals(other.nonWifiThresholdTimeInSeconds))
            return false;
        if (obssHopMode != other.obssHopMode)
            return false;
        return true;
    }

    @Override
    public boolean hasUnsupportedValue() 
    {
        return OBSSHopMode.isUnsupported(obssHopMode);
    }
}
