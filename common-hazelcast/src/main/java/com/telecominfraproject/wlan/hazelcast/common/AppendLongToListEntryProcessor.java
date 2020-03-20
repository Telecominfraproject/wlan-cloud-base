package com.telecominfraproject.wlan.hazelcast.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.hazelcast.map.AbstractEntryProcessor;

/**
 * This class appends an item to a List<Long> stored in a hazelcast map.
 * Usage pattern:
 * <pre>
 * IMap&lt;String, List&lt;Long>> tsMap = hazelcastClient.getMap(tsMapName);
 * tsMap.submitToKey(tsKey, new AppendLongToListEntryProcessor(createdTimestampToAppend) ).get();
 * </pre>
 * <b>Very important</b>: this class must implement Serializable interface because it is submitted to Hazelcast Cluster
 * @author dtop
 */
public class AppendLongToListEntryProcessor extends AbstractEntryProcessor<String, List<Long>> implements Serializable {
    private static final long serialVersionUID = -6960225265547599510L;
    
    private long tsToAppend; 
    
    public AppendLongToListEntryProcessor() {
        // for serialization
    }

    public AppendLongToListEntryProcessor(long tsToAppend) {
        this.tsToAppend = tsToAppend;
    }

    @Override
    public Object process(Entry<String, List<Long>> entry) {
        List<Long> value = entry.getValue();
        
        if(value==null){
            value = new ArrayList<>();
        }
        
        // process and modify value
        if(!value.contains(tsToAppend)){ 
            value.add(tsToAppend); 
        } 
        entry.setValue(value);

        return true;
    }
}