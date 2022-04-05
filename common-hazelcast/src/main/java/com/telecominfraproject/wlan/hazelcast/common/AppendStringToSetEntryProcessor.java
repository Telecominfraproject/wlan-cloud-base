package com.telecominfraproject.wlan.hazelcast.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.hazelcast.map.impl.ComputeEntryProcessor;

/**
 * This class appends an item to a Set<String> stored in a hazelcast map.
 * Usage pattern:
 * <pre>
 * IMap&lt;String, Set&lt;String>> dirListMap = hazelcastClient.getMap(dirListMapName);
 * dirListMap.submitToKey(dirKey, new AppendStringToSetEntryProcessor(stringToAppend) ).get();
 * </pre>
 * <b>Very important</b>: this class must implement Serializable interface because it is submitted to Hazelcast Cluster
 * @author dtop
 */
public class AppendStringToSetEntryProcessor extends ComputeEntryProcessor<String, Set<String>> implements Serializable {
    private static final long serialVersionUID = -6960225265547599510L;
    
    private String stringToAppend; 
    
    public AppendStringToSetEntryProcessor() {
        // for serialization
    }

    public AppendStringToSetEntryProcessor(String stringToAppend) {
        this.stringToAppend = stringToAppend;
    }

    @Override
    public Set<String> process(Entry<String, Set<String>> entry) {
        Set<String> value = entry.getValue();
        
        if(value==null){
            value = new HashSet<>();
        }
        
        // process and modify value
        value.add(stringToAppend);
        
        entry.setValue(value);

        return value;
    }
}