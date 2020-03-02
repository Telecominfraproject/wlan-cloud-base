package com.telecominfraproject.wlan.core.model.converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies a list of converters to incoming model. Only first matching converter in the list is used.
 * 
 * @author dtop
 *
 * @param <F>
 * @param <T>
 */
public class ConverterList<F, T> extends BaseModelConverter<F, T> {
    
    private List<BaseModelConverter<F,T>> converters = new ArrayList<>();
    
    public ConverterList(List<BaseModelConverter<F,T>> converters) {
        this.converters.addAll(converters);
    }
    
    @Override
    public boolean canProcess(F fromModel) {
        for(BaseModelConverter<F,T> c: converters){
            if(c.canProcess(fromModel)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Only first matching converter will be used.
     * 
     * @see com.telecominfraproject.wlan.core.model.converters.BaseModelConverter#convertDirect(com.telecominfraproject.wlan.core.model.json.BaseJsonModel)
     */
    @Override
    protected T convertDirect(F fromModel) {
        for(BaseModelConverter<F,T> c: converters){
            if(c.canProcess(fromModel)){
                return c.convertDirect(fromModel);
            }
        }
        return null;
    }
    
}
