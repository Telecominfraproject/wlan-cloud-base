package com.whizcontrol.core.model.converters;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * Does nothing, returns the same BaseJsonModel.
 * 
 * @author dtop
 *
 * @param <F>
 */
public class EmptyBaseModelConverter<F extends BaseJsonModel> extends EmptyConverter<F, BaseJsonModel> {
    
}
