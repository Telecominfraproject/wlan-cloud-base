package com.telecominfraproject.wlan.core.model.equipment;

public class SourceSelectionValue extends AbstractSource<Integer>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4631172351117490997L;
	
	private SourceSelectionValue() {

	}

    private SourceSelectionValue(SourceType source, int value) {
        super(source, value);
    }

    public static SourceSelectionValue createAutomaticInstance(int value) {
        return new SourceSelectionValue(SourceType.auto, value);
    }

    public static SourceSelectionValue createManualInstance(int value) {
        return new SourceSelectionValue(SourceType.manual, value);
    }
    
    public static SourceSelectionValue createProfileInstance(int value) {
        return new SourceSelectionValue(SourceType.profile, value);
    }
    
    public static AutoOrManualValue getAutoOrManualFromSourcedValue(SourceSelectionValue param) {
    	AutoOrManualValue ret = null;
    	if (param.getSource() == SourceType.auto) {
    		ret = AutoOrManualValue.createAutomaticInstance(param.getValue());
    	} else if (param.getSource() == SourceType.profile) {
    		ret = AutoOrManualValue.createManualInstance(param.getValue());
    	} else { // else param.getSource == SourceType.manual
    		ret = AutoOrManualValue.createManualInstance(param.getValue());
    	}
    	return ret;
    }   
    
    @Override
    public boolean hasUnsupportedValue() {
        if (SourceType.isUnsupported(source)) {
            return true;
        }
        return false;
    }

}
