package com.telecominfraproject.wlan.core.model.equipment;

public class SourceSelectionSteering extends AbstractSource<RadioBestApSettings>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4631172351117490997L;
	
	private SourceSelectionSteering() {

	}

    private SourceSelectionSteering(SourceType source, RadioBestApSettings value) {
        super(source, value);
    }

    public static SourceSelectionSteering createAutomaticInstance(RadioBestApSettings value) {
        return new SourceSelectionSteering(SourceType.auto, value);
    }

    public static SourceSelectionSteering createManualInstance(RadioBestApSettings value) {
        return new SourceSelectionSteering(SourceType.manual, value);
    }
    
    public static SourceSelectionSteering createProfileInstance(RadioBestApSettings value) {
        return new SourceSelectionSteering(SourceType.profile, value);
    }
	
    @Override
    public boolean hasUnsupportedValue() {
        if (SourceType.isUnsupported(source)) {
            return true;
        }
        if ((null != value) && value.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }
    
}
