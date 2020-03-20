package com.telecominfraproject.wlan.hazelcast.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.impl.MapService;
import com.telecominfraproject.wlan.core.model.filter.EntryFilter;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.json.interfaces.HasProducedTimestamp;

public class HazelcastUtils {

    public static <T extends BaseJsonModel> List<T> getModelsFromHazelcastByMapPrefix(HazelcastInstance hazelcastInstance, String mapPrefix, long fromTime, long toTime, EntryFilter<T> entryFilter){
        if(hazelcastInstance==null){
            return Collections.emptyList();
        }
        
        //Need to be more specific with the map names. 
        //Without delimiter a request for se_1 will pick up se_1_1, se_1_2 (which is fine), 
        //  but it will also pick up se_10_1, se114_3 (which is wrong)
        String mapPrefixWithDelimiter = mapPrefix + "_";
        
        List<String> matchingMaps = new ArrayList<>();
        for(DistributedObject distObj: hazelcastInstance.getDistributedObjects()){
            if(distObj.getServiceName().equals(MapService.SERVICE_NAME) && 
                    (distObj.getName().equals(mapPrefix) || distObj.getName().startsWith(mapPrefixWithDelimiter))){
                matchingMaps.add(distObj.getName());
            }
        }

        List<T> ret = new ArrayList<>();
        
        for(String mapName: matchingMaps){
            ret.addAll(getModelsFromHazelcast(hazelcastInstance, mapName, fromTime, toTime, entryFilter));
        }
        
        return ret;
    }
    
    public static <T extends BaseJsonModel> List<T> getModelsFromHazelcast(HazelcastInstance hazelcastInstance, String mapName, long fromTime, long toTime, EntryFilter<T> entryFilter){
        if(hazelcastInstance==null){
            return Collections.emptyList();
        }
        
        //if hazelcast datagrid is configured, retrieve records from it
        Map<Long, byte[]> reeMap = hazelcastInstance.getMap(mapName);

        List<T> ret = new ArrayList<>();
        Iterator<Map.Entry<Long, byte[]>> iterator = reeMap.entrySet().iterator();
        Map.Entry<Long, byte[]> entry;
        while(iterator.hasNext()){
            entry = iterator.next();
            @SuppressWarnings("unchecked")
            T ree = (T) BaseJsonModel.fromZippedBytes(entry.getValue(), BaseJsonModel.class);
            
            if(ree instanceof HasProducedTimestamp){
                HasProducedTimestamp record = (HasProducedTimestamp) ree; 
                if(record.getProducedTimestampMs()>=fromTime && record.getProducedTimestampMs()<=toTime){
                    T filteredRee = entryFilter.getFilteredEntry(ree);
                    if(filteredRee!=null){
                        ret.add(filteredRee);
                    }
                }
            }
        }
        
        return ret;
    }
    
    public static <T extends BaseJsonModel> int countModels(HazelcastInstance hazelcastInstance, String mapName, long fromTime, long toTime, EntryFilter<T> entryFilter){
        return getModelsFromHazelcast(hazelcastInstance, mapName, fromTime, toTime, entryFilter).size();
    } 

    public static <T extends BaseJsonModel> int countModelsByMapPrefix(HazelcastInstance hazelcastInstance, String mapPrefix, long fromTime, long toTime, EntryFilter<T> entryFilter){
        return getModelsFromHazelcastByMapPrefix(hazelcastInstance, mapPrefix, fromTime, toTime, entryFilter).size();
    } 

    /**
     * @param list
     * @return max timestamp from the supplied list, or -1 if list is empty/null
     */
    public static <T extends HasProducedTimestamp> long getMaxTimestamp(List<T> list){
        long ret=-1;
        
        if(list==null){
            return ret;
        }
        
        for(T item: list){
            if(item.getProducedTimestampMs()>ret){
                ret = item.getProducedTimestampMs();
            }
        }
        
        return ret;
    }
    
    /**
     * @param destination
     * @param source
     * Copies items from the source list into destination list, but only those that have timestamp greater than max timestamp of the original destination list.
     * This is to deal with duplicate records (records with the same timestamp) that are present in both S3 and Hazelcast results
     */
    public static <T extends HasProducedTimestamp> void combineLists(List<T> destination, List<T> source){
        long maxTs = getMaxTimestamp(destination);

        Set<T> overlappingItems = new HashSet<>();
        //collect items from the destination that have maxTs - we'll use them to detect duplicates when merging lists
        for(T item: destination){
            if(item.getProducedTimestampMs()==maxTs){
                overlappingItems.add(item);
            }
        }
        
        for(T item: source){
            if(item.getProducedTimestampMs()>maxTs || ( item.getProducedTimestampMs()==maxTs && !overlappingItems.contains(item) ) ){
                destination.add(item);
            }
        }
    }
    
    /**
     * Build a DatagridMapName using the supplied mapPrefix, customerId and equipmentId.
     * @param mapPrefix
     * @param customerId
     * @param equipmentId
     * @return
     */
    public static String getDatagridMapName(String mapPrefix, int customerId, long equipmentId){
        return getDatagridMapCustomerPrefix(mapPrefix, customerId)+"_"+equipmentId;
    }

    /**
     * Build a DatagridMapName using the supplied mapPrefix, and customerId.
     * @param mapPrefix
     * @param customerId
     * @return
     */
    public static String getDatagridMapCustomerPrefix(String mapPrefix, int customerId){
        return mapPrefix + customerId;
    }


    
}
