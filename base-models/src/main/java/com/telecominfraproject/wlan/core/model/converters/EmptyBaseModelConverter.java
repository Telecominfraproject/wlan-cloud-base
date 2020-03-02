package com.telecominfraproject.wlan.core.model.converters;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * Does nothing, returns the same BaseJsonModel.
 * 
 * @author dtop
 *
 * @param <F>
 */
public class EmptyBaseModelConverter<F extends BaseJsonModel> extends EmptyConverter<F, BaseJsonModel> {
    
}
