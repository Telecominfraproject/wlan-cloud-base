package com.whizcontrol.core.model.equipment;

import com.whizcontrol.core.model.json.BaseJsonModel;

public class RadioBestApSettingsPerRadio extends BaseJsonModel 
{
    /**
     * 
     */
    private static final long serialVersionUID = -4777181913584557893L;
    private RadioType radioType;
    private RadioBestApSettings settings;
    
    public RadioBestApSettingsPerRadio()
    {
        //
    }

    public RadioBestApSettingsPerRadio(RadioType radioType, RadioBestApSettings settings) {
        this();
        this.radioType = radioType;
        this.settings = settings;
    }

    public RadioType getRadioType() {
        return radioType;
    }

    public void setRadioType(RadioType radioType) {
        this.radioType = radioType;
    }

    public RadioBestApSettings getSettings() {
        return settings;
    }

    public void setSettings(RadioBestApSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean hasUnsupportedValue() {
        if (RadioType.isUnsupported(radioType)) {
            return true;
        }
        if ((null != settings) && settings.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }

}
