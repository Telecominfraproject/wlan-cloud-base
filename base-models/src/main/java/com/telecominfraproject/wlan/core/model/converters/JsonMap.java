package com.telecominfraproject.wlan.core.model.converters;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * Generic class used to produce metric data with arbitrary properties. 
 * Used by model converters for ML algorithms to minimize size of the data files.
 *  
 * @author dtop
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class JsonMap extends BaseJsonModel {

	private static final long serialVersionUID = 1502983533893669382L;
	
	private Map<String, Object> m = new HashMap<>();
    
    public Map<String, Object> getM() {
        return m;
    }

    public void setM(Map<String, Object> m) {
        this.m = m;
    }

}
