package com.whizcontrol.core.model.converters;

/**
 * Does nothing, returns the same model.
 * 
 * @author dtop
 *
 * @param <F>
 */
public class EmptyConverter<F extends T, T> extends BaseModelConverter<F, T> {
    
    @Override
    public boolean canProcess(F fromModel) {
        return true;
    }
    
    /**
     * @see com.whizcontrol.core.model.converters.BaseModelConverter#convertDirect(com.whizcontrol.core.model.json.BaseJsonModel)
     */
    @Override
    protected T convertDirect(F fromModel) {
        return fromModel;
    }
    
}
