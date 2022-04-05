package com.telecominfraproject.wlan.hazelcast.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import com.hazelcast.map.impl.ComputeEntryProcessor;


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
public class AppendLongToListEntryProcessor extends ComputeEntryProcessor<String, List<Long>> implements Serializable {
    private static final long serialVersionUID = -6960225265547599510L;

    private long tsToAppend; 

//    private BiFunction<String, List<Long>, List<Long>> biFunction =  (key, value) -> {
//        if(value==null){
//            value = new ArrayList<>();
//        }
//        
//        // process and modify value
//        if(!value.contains(tsToAppend)){ 
//            value.add(tsToAppend); 
//        } 
//        
//        return value;
//    };
        
    public AppendLongToListEntryProcessor() {
        // for serialization
    }

    public AppendLongToListEntryProcessor(long tsToAppend) {
        this.tsToAppend = tsToAppend;
    }

    @Override
    public List<Long> process(Entry<String, List<Long>> entry) {
        List<Long> value = entry.getValue();
        
        if(value==null){
            value = new ArrayList<>();
        }
        
        // process and modify value
        if(!value.contains(tsToAppend)){ 
            value.add(tsToAppend); 
        } 
        entry.setValue(value);

        return value;
    }
}