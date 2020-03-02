package com.telecominfraproject.wlan.core.model.converters;

/**
 * Base class for model converters - components that transform model of one class into model of another one.
 * Model converters are initially used when streaming large datasets - to include only those properties of 
 *  the original models that are used in later processing.
 *  
 * @author dtop
 * 
 * @param <F> class to convert model from
 * @param <T> class to convert model to
 */
public abstract class BaseModelConverter<F, T> {
    
    /**
     * @param fromModel - incoming model of class F
     * @return outgoing model of class T, or null if F cannot be converted to T (canProcess returns false) 
     */
    public T convert(F fromModel){
        if(canProcess(fromModel)){
            return convertDirect(fromModel);
        } else {
            return null;
        }
    }

    protected abstract T convertDirect(F fromModel);

    
    /**
     * @param fromModel
     * @return true if this converter will process supplied model (by calling its convert() method), false otherwise
     */
    public abstract boolean canProcess(F fromModel);
    
    
}
