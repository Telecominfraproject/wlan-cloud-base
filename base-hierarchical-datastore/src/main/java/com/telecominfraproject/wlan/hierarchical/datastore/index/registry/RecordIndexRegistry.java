package com.telecominfraproject.wlan.hierarchical.datastore.index.registry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexValueExtractor;

/**
 * This class defines all record indexes used by hds in our system
 * 
 * @author dtop
 *
 */
@Component
public class RecordIndexRegistry {

	//dtop: TODO: move these commented out parts into appropriate descendants of the hierarchical datastore
/*	
    public static enum EquipmentEventsIndex{
        eventType,
        clientMac
    }

    public static enum SystemEventsIndex{
        payloadType,
        clientMac
    }

    public static enum ServiceMetricsIndex{
        dataType,
        clientMac
    }

    public static enum SingleValueMetricIndex{
        metricDataType;
    }

    @Value("${whizcontrol.RawEquipmentEventDatastore.s3ds.fileNamePrefix:ree}")
    private String reeFileNamePrefix;

    @Value("${whizcontrol.SystemEventDatastore.s3ds.fileNamePrefix:se}")
    private String seFileNamePrefix;
    
    @Value("${whizcontrol.ServiceMetricsDatastore.s3ds.fileNamePrefix:sm}")
    private String smFileNamePrefix;
*/

    private Map<String, Map<String, RecordIndexValueExtractor>> fullIndexMap = new HashMap<>();
    private Set<String> fileNamePrefixes = new HashSet<>();
    
    @PostConstruct
    public void postConstruct(){
        //we need for property values to be resolved in order to initialize internal maps
        
    	//dtop: TODO: move these commented out parts into appropriate descendants of the hierarchical datastore
/*
        //for testing or running outside of spring environment
        reeFileNamePrefix = reeFileNamePrefix!=null?reeFileNamePrefix:"ree";
        seFileNamePrefix = seFileNamePrefix!=null?seFileNamePrefix:"se";
        smFileNamePrefix = smFileNamePrefix!=null?smFileNamePrefix:"sm";
        
        Map<String, RecordIndexValueExtractor> reeIndexes = new HashMap<>();
        Map<String, RecordIndexValueExtractor> seIndexes = new HashMap<>();
        Map<String, RecordIndexValueExtractor> smIndexes = new HashMap<>();
        Map<String, RecordIndexValueExtractor> smSingleValueIndexes = new HashMap<>();
        
        reeIndexes.put(EquipmentEventsIndex.eventType.toString(), new RecordIndexValueExtractor() {           
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                if(model instanceof BaseRawEquipmentEvent){
                    return Collections.singleton(((BaseRawEquipmentEvent)model).getEventTypeName());
                } 
                return Collections.singleton("");
            }
        });
        
        reeIndexes.put(EquipmentEventsIndex.clientMac.toString(), new RecordIndexValueExtractor() {           
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                if(model instanceof ClientRawEventInterface ){
                    ClientRawEventInterface crei = (ClientRawEventInterface) model;
                    MacAddress mac = MacAddress.valueOf(crei.getDeviceMacAddress());
                    
                    if(mac != null) 
                    {
                        Long macAddress = mac.getAddressAsLong();
                        
                        if(macAddress != null)
                        {
                            return Collections.singleton(macAddress.toString());
                        }
                        
                    }
                }
                
                return Collections.singleton("");
            }
        });
    
        seIndexes.put(SystemEventsIndex.payloadType.toString(), new RecordIndexValueExtractor() {           
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                if(model instanceof SystemEvent){
                    return Collections.singleton(model.getClass().getSimpleName());
                }
                return Collections.singleton("");
            }
        });

        seIndexes.put(SystemEventsIndex.clientMac.toString(), new RecordIndexValueExtractor() {           
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                if(model instanceof ClientSystemEventInterface ){
                    ClientSystemEventInterface csei = (ClientSystemEventInterface) model;
                    MacAddress mac = MacAddress.valueOf(csei.getDeviceMacAddress());
                    
                    if(mac != null) 
                    {
                        Long macAddress = mac.getAddressAsLong();
                        
                        if(macAddress != null)
                        {
                            return Collections.singleton(macAddress.toString());
                        }
                        
                    }
                }
                
                return Collections.singleton("");
            }
        });
        
        smIndexes.put(ServiceMetricsIndex.dataType.toString(), new RecordIndexValueExtractor() {           
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                if(model instanceof SingleMetricRecord){
                    return Collections.singleton(((SingleMetricRecord) model).getDataType());
                }
                return Collections.singleton("");
            }
        });
        
        smIndexes.put(ServiceMetricsIndex.clientMac.toString(), new RecordIndexValueExtractor() {           
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                //here we are dealing with multi-value indexes - one record contains multiple MAC addresses
                if(model instanceof SingleMetricRecord){
                    if(((SingleMetricRecord)model).getData() instanceof ClientMetricsInterface ){
                        ClientMetricsInterface cmi = (ClientMetricsInterface) ((SingleMetricRecord)model).getData();
                        Set<String> ret = new HashSet<>();
                        
                        for(MacAddress mac: cmi.getDeviceMacAddresses())
                        {
                            Long macAddress = mac.getAddressAsLong();
                            
                            if(macAddress != null)
                            {
                                ret.add(macAddress.toString());
                            }
                            
                        }
                        
                        if(!ret.isEmpty()){
                            return ret;
                        }
                    }
                }
                
                return Collections.singleton("");
            }
        });   
        
        smSingleValueIndexes.put(SingleValueMetricIndex.metricDataType.toString(), new RecordIndexValueExtractor() {           
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                if(model instanceof SingleValueMetric){
                    return Collections.singleton(((SingleValueMetric) model).getMetricId().toString());
                }
                return Collections.singleton("");
            }
        });
        
        fullIndexMap.put(reeFileNamePrefix, reeIndexes);
        fullIndexMap.put(seFileNamePrefix, seIndexes);
        fullIndexMap.put(smFileNamePrefix, smIndexes);
        fullIndexMap.put(smFileNamePrefix + TieredAggregationTable.table_x5m.getTableSuffix(), smSingleValueIndexes);
        fullIndexMap.put(smFileNamePrefix + TieredAggregationTable.table_x15m.getTableSuffix(), smSingleValueIndexes);
        fullIndexMap.put(smFileNamePrefix + TieredAggregationTable.table_x30m.getTableSuffix(), smSingleValueIndexes);
        fullIndexMap.put(smFileNamePrefix + TieredAggregationTable.table_x1h.getTableSuffix(), smSingleValueIndexes);
        fullIndexMap.put(smFileNamePrefix + TieredAggregationTable.table_x4h.getTableSuffix(), smSingleValueIndexes);
        fullIndexMap.put(smFileNamePrefix + TieredAggregationTable.table_x24h.getTableSuffix(), smSingleValueIndexes);
        
        //keep track of all known file name prefixes - used to identify data files 
        fileNamePrefixes.add(reeFileNamePrefix);
        fileNamePrefixes.add(reeFileNamePrefix);
        fileNamePrefixes.add(seFileNamePrefix);
        fileNamePrefixes.add(smFileNamePrefix);
        fileNamePrefixes.add(smFileNamePrefix + TieredAggregationTable.table_x5m.getTableSuffix());
        fileNamePrefixes.add(smFileNamePrefix + TieredAggregationTable.table_x15m.getTableSuffix());
        fileNamePrefixes.add(smFileNamePrefix + TieredAggregationTable.table_x30m.getTableSuffix());
        fileNamePrefixes.add(smFileNamePrefix + TieredAggregationTable.table_x1h.getTableSuffix());
        fileNamePrefixes.add(smFileNamePrefix + TieredAggregationTable.table_x4h.getTableSuffix());
        fileNamePrefixes.add(smFileNamePrefix + TieredAggregationTable.table_x24h.getTableSuffix());
        */
    }
    
    public Map<String, RecordIndexValueExtractor> getIndexMap(String fileNamePrefix){
        Map<String, RecordIndexValueExtractor> ret = fullIndexMap.get(fileNamePrefix);
        if(ret == null){
            ret = new HashMap<>();
            fullIndexMap.put(fileNamePrefix, ret);
        }
        return ret;
    }
    
    public Set<String> getAllIndexesForFileNamePrefix(String fileNamePrefix){
        return getIndexMap(fileNamePrefix).keySet();
    }

    public RecordIndexValueExtractor getIndexValueExtractor(String fileNamePrefix, String indexName){
        Map<String, RecordIndexValueExtractor> idxMap = getIndexMap(fileNamePrefix);
        return idxMap!=null?idxMap.get(indexName):null;
    }

    public Set<String> getAllFileNamePrefixes(){
        return fileNamePrefixes;
    }
}
