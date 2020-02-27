package com.whizcontrol.core.model.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * @author dtop
 * Generic interface that allows arbitrary filtering of models.
 * @param <T>
 */
public abstract class EntryFilter<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(EntryFilter.class);

    /**
     * @param entryString
     * @return filtered entry created (parsed) from supplied entryString, 
     *  potentially modified, or null if entry does not match the filter
     */
    public final T getFilteredEntry(String entryStr, Class<T> dataClass){
        //deserialize string into JSON
        try{
            BaseJsonModel obj = BaseJsonModel.fromString(entryStr, BaseJsonModel.class);
            if (!dataClass.isInstance(obj)) {
                return null;
            }
            return getFilteredEntry(dataClass.cast(obj));
        }catch(Exception e){
            //cannot deserialize entry, return null
            LOG.warn("Could not deserialize entry {}", entryStr, e);
            return null;
        }
    }
    
    /**
     * @param entry
     * @return filtered entry, potentially modified, or null if entry does not match the filter
     */
    public abstract T getFilteredEntry(T entry);

}
