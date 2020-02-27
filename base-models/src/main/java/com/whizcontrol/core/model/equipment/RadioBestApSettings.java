package com.whizcontrol.core.model.equipment;

import com.whizcontrol.core.model.json.BaseJsonModel;

public class RadioBestApSettings extends BaseJsonModel 
{
    /**
     * 
     */
    private static final long serialVersionUID = 8878291963217534078L;

    private boolean mlComputed;
    private Integer dropInSnrPercentage;
    private Integer minLoadFactor;

    public RadioBestApSettings()
    {
        super();
        this.dropInSnrPercentage = 40;
        this.mlComputed = true;
    }

    public RadioBestApSettings(Integer dropInSnrPercentage, Integer minLoadFactor) 
    {
        this();
        this.dropInSnrPercentage = dropInSnrPercentage;
        this.minLoadFactor = minLoadFactor;
    }

    public Integer getDropInSnrPercentage() {
        return dropInSnrPercentage;
    }

    public void setDropInSnrPercentage(Integer dropInSnrPercentage) {
        this.dropInSnrPercentage = dropInSnrPercentage;
    }

    public Integer getMinLoadFactor() {
        return minLoadFactor;
    }

    public void setMinLoadFactor(Integer minLoadFactor) {
        this.minLoadFactor = minLoadFactor;
    }

    public boolean isMlComputed() {
        return mlComputed;
    }

    public void setMlComputed(boolean mlComputed) {
        this.mlComputed = mlComputed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dropInSnrPercentage == null) ? 0 : dropInSnrPercentage.hashCode());
        result = prime * result + ((minLoadFactor == null) ? 0 : minLoadFactor.hashCode());
        result = prime * result + (mlComputed ? 1231 : 1237);
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
        RadioBestApSettings other = (RadioBestApSettings) obj;
        if (dropInSnrPercentage == null) {
            if (other.dropInSnrPercentage != null)
                return false;
        } else if (!dropInSnrPercentage.equals(other.dropInSnrPercentage))
            return false;
        if (minLoadFactor == null) {
            if (other.minLoadFactor != null)
                return false;
        } else if (!minLoadFactor.equals(other.minLoadFactor))
            return false;
        if (mlComputed != other.mlComputed)
            return false;
        return true;
    }

    public static RadioBestApSettings createWithDefaults(RadioType radioType) 
    {
        if(radioType == RadioType.is2dot4GHz)
        {
            return new RadioBestApSettings(20, 50);
        }
        else
        {
            return new RadioBestApSettings(30, 40);
        }
    }

    @Override
    public boolean hasUnsupportedValue() {
        return false;
    }

}
