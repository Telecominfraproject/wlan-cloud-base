package com.whizcontrol.core.model.json.flattener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * This class produces a flattened map from a json document. Used to create matrixes for machine learning algorithms.
 * 
 * @author dtop
 *
 */
public class ModelFlattener {

    private static final Logger LOG = LoggerFactory.getLogger(ModelFlattener.class);
    
    //data.clientMetrics5g[2].deviceMacAddress.address : "vFQ2PVn4" ==> data.clientMetrics5g[2].* ==>> data.clientMetrics5g."vFQ2PVn4".*
    // that looks better:
    //data.clientMetrics5g[2].deviceMacAddress.addressAsString : "bc:54:36:3d:59:f8" ==> data.clientMetrics5g[2].* ==>> data.clientMetrics5g."bc:54:36:3d:59:f8".*
    //Patterns are thread safe        
    private static final Pattern[] patternsToTranslate = {
            Pattern.compile("^((data.clientMetrics[25]g)\\[\\d+\\]).deviceMacAddress.addressAsString$"),
            Pattern.compile("^((data.ssidStats[25]g)\\[\\d+\\]).bssid.addressAsString$"),
            Pattern.compile("^((equipmentScanRecord\\..+\\.bssIds)\\[\\d+\\]).macAddress$"),
            Pattern.compile("^((equipmentScanRecord.scanDetails.managedNeighbours\\.\\d+)\\[\\d+\\]).equipmentId$"),
            Pattern.compile("^((equipmentScanRecord.scanDetails.unmanagedNeighbours)\\[\\d+\\])\\.radios\\[\\d+\\].channel$"),
            Pattern.compile("^((equipmentScanRecord.scanDetails.unmanagedNeighbours\\[\\d+\\]\\.radios)\\[\\d+\\]).radioType$"),
            Pattern.compile("^((data.neighbourReports)\\[\\d+\\])\\.macAddress$"),
            Pattern.compile("^((.+\\.channelInformationReports[25]g)\\[\\d+\\])\\.chanNumber$"),
            Pattern.compile("^((settings)\\[\\d+\\]).radioType$"),
            };

    private static final Pattern macPattern = Pattern.compile(":");
    private static final Pattern commaPattern = Pattern.compile(",");
    
    private static final String[] keysToIgnore = {"_type", "apName", "reportType", "dataType", "data.type", "id", 
            "qrCode", "customerId", "equipmentId", "lastModifiedTimestampStr", "createdTimestampStr", 
            "processingStartTime",  
            };
    private static final Set<String> keysToIgnoreSet = new HashSet<>(Arrays.asList(keysToIgnore));
    
    public static Map<String, Object> flattenJson(String jsonStr, List<Pattern> pathPatternsToInclude, List<Pattern> pathPatternsToExclude, boolean splitMacAddressesIntoBytes) {
        Map<String, Object> map = new HashMap<>();
        try {
            recursiveProcessJson("", new ObjectMapper().readTree(jsonStr), map, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
            map = correctMapKeys(map);
        } catch (IOException e) {
            LOG.error("Cannot flatten JSON", e);
        }
        return map;
    }

    /**
     * @param currentPath
     * @param jsonNode
     * @param map
     * @param pathPatternsToInclude - If null or empty, then include everything into resulting map. 
     *      Otherwise, only values with paths that match specified regexps will be included into the map.
     *      This parameter is applied only to the leaf nodes of the json tree, after excludes are processed.  
     * @param pathPatternsToExclude - If null or empty, then include everything into resulting map. 
     *      Otherwise, exclude from resulting map values with paths that match specified regexps. 
     *      This parameter is applied to all nodes of the json tree and allows to efficiently skip processing of entire branches of the json tree.
     *      
     * If a single path matches both Exclude and Include paths, then Exclude takes preference and the path will not be included into the resulting map.   
     */
    private static void recursiveProcessJson(String currentPath, JsonNode jsonNode, Map<String, Object> map, List<Pattern> pathPatternsToInclude, List<Pattern> pathPatternsToExclude, boolean splitMacAddressesIntoBytes) {
        
        if(pathPatternsToExclude!=null && !pathPatternsToExclude.isEmpty()){
            boolean skipThisPath = false;
            Matcher matcher;
            for(Pattern pattern: pathPatternsToExclude){
                matcher = pattern.matcher(currentPath);
    
                if(matcher.matches()){
                    //at least one ignore pattern matched, so skip this branch 
                    skipThisPath = true;
                    break;
                }
            }
            
            if(skipThisPath){
                //skip this branch, because we explicitly do not want it
                return;          
            }
        }

        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                recursiveProcessJson(pathPrefix + entry.getKey(), entry.getValue(), map, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            int i = 0;
            for(JsonNode jnode: arrayNode){
                recursiveProcessJson(currentPath + "[" + i + "]", jnode, map, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
                i++;
            }
        } else if (jsonNode.isValueNode()) {
            if(currentPath.endsWith("apPerformance.cpuUtilizedStr") 
                    || currentPath.endsWith(".rates")
                    || currentPath.endsWith(".mcs")
                    ){
                //skip these values, they are redundant
                return;
            }
            
            if(pathPatternsToInclude!=null && !pathPatternsToInclude.isEmpty()){
                boolean skipThisPath = true;
                Matcher matcher;
                for(Pattern pattern: pathPatternsToInclude){
                    matcher = pattern.matcher(currentPath);
        
                    if(matcher.matches()){
                        //at least one pattern matched, so do not skip this value
                        skipThisPath = false;
                        break;
                    }
                }
                
                if(skipThisPath){
                    //skip these values, because we explicitly do not want them
                    return;          
                }
            }

            ValueNode valueNode = (ValueNode) jsonNode;
            if(currentPath.endsWith("apPerformance.cpuUtilized")){
                //special treatment - this value is a base-64 encoded string that represents an array of cpu utilizations
                //decode base64 and put the individual values in the resulting map
                byte[] cpuUtils = Base64Utils.decodeFromString(valueNode.asText());
                if(cpuUtils!=null){
                    int i = 0;
                    for(byte cu: cpuUtils){
                        map.put(currentPath + "[" + i + "]", cu);
                        i++;
                    }
                }
                
            } else if(currentPath.endsWith("addressAsString")){
                if(splitMacAddressesIntoBytes){
                    //special treatment - this value is a MAC address, we want to deal with individual bytes 
                    //of it to help find patterns related to manufacturer, etc.
                    //Parse MAC string and put the individual values in the resulting map
                    String[] macElements = macPattern.split(valueNode.asText());
                    if(macElements!=null){
                        int i = 0;
                        for(String me: macElements){
                            //remove ".addressAsString" from the column name, use .mac instead
                            map.put(currentPath.substring(0, currentPath.length()-15) + "mac[" + i + "]", Integer.parseInt(me, 16));
                            i++;
                        }
                    }
                }
                
                //need to put the whole MAC in the map for future translation of array indexes
                map.put(currentPath, valueNode.asText());
            } else if(currentPath.endsWith("ratesStr") || currentPath.endsWith("mcsStr")){
                //special treatment - this value is a String-encoded array [1,2,3], we want to deal with individual elements 
                //of it to help find patterns. 
                //Parse array string and put the individual values in the resulting map
                String[] arrayElements = commaPattern.split(valueNode.asText());
                if(arrayElements!=null && arrayElements.length>0){
                    //remove [ from first element and ] from the last one
                    arrayElements[0] = arrayElements[0].substring(1);
                    arrayElements[arrayElements.length-1] = arrayElements[arrayElements.length-1].substring(0, arrayElements[arrayElements.length-1].length() - 1);
                    
                    int i = 0;
                    for(String me: arrayElements){
                        //remove ".ratesStr" or ".mcsStr" from the column name, use .rates or .mcs instead"
                        map.put(currentPath.substring(0, currentPath.length()-3) + "[" + i + "]", Integer.parseInt(me));
                        i++;
                    }
                }
                                
            } else {
                if(valueNode.isNumber()){
                    map.put(currentPath, valueNode.numberValue());
                } else {
                    map.put(currentPath, valueNode.asText());
                }
            }
        }
    }
    
    private static Map<String, Object> correctMapKeys(Map<String, Object> map){
        if(map == null){
            return new HashMap<>();
        }
        
        Map<String, Object> ret = new HashMap<>(); 
    
        //for arrays that should have been defined as maps - correct flattened out json keys to reflect their identity, as opposed to just the array index
        // without this correction device macs and AP bssids from different metric reports will be all mixed up as they can show up at different positions in arrays
        Map<String, String> translationMap = new HashMap<>();
        String key;
                
        //build translation map
        Matcher matcher; 
        for(Map.Entry<String, Object> entry: map.entrySet()){
            key = entry.getKey();
            
            for(Pattern pattern: patternsToTranslate){
                matcher = pattern.matcher(key);
    
                if(matcher.matches()){                   
                    //data.clientMetrics5g[2].deviceMacAddress.address : "vFQ2PVn4" ==> data.clientMetrics5g[2].* ==>> data.clientMetrics5g."vFQ2PVn4".*
                    // that looks better:
                    //data.clientMetrics5g[2].deviceMacAddress.addressAsString : "bc:54:36:3d:59:f8" ==> data.clientMetrics5g[2].* ==>> data.clientMetrics5g."bc:54:36:3d:59:f8".*
                    //Example Pattern
                    //  ^((data.clientMetrics5g)\\[\\d+\\]).deviceMacAddress.addressAsString$
                    
                    //one key matches only one translation pattern (the first in the list)
                    translationMap.put(matcher.group(1), matcher.group(2) + ".\"" + String.valueOf(entry.getValue()) + "\"");
                    break;
                }
            }
        }
       
        String commonTypePrefix = (String) map.get("_type");

        if(commonTypePrefix == null){
            commonTypePrefix = "";
        } else {
            
            if("SingleMetricRecord".equals(commonTypePrefix)){
                commonTypePrefix = (String) map.get("data._type");
            }

            commonTypePrefix += ".";
            
            Number equipmentId = (Number) map.get("equipmentId");
            if(equipmentId!=null){
                commonTypePrefix += equipmentId.toString() + ".";
            }
        }
        
        
        //apply translation map
        
        //need to use reverse-sorted translation keys so that shorter keys are applied later than the longer ones
        //this is done so that we can perform multiple translations for a single key
        List<String> sortedTranslationKeys = new ArrayList<>(translationMap.keySet());
        Collections.sort(sortedTranslationKeys);
        Collections.reverse(sortedTranslationKeys);
        
        for(Map.Entry<String, Object> entry: map.entrySet()){
            key = entry.getKey();
            boolean keyWasTranslated;
            boolean keepTranslating = true;
            int translationCount = 0;
            int translationLimit = 10;
            
            //multiple translation patterns can affect single key, for example equipmentId and macAddress can be both part of the key
            //we want to keep translating until there's no more translation happening (up to a limit)            
            while(keepTranslating){
                keyWasTranslated = false;
                
                for(String translationEntryKey: sortedTranslationKeys){
                    
                    if(key.startsWith(translationEntryKey)){
                        key = translationMap.get(translationEntryKey) + key.substring(translationEntryKey.length());
                        keyWasTranslated = true;
                        break;
                    }
                }
                
                if(!keyWasTranslated){
                    keepTranslating = false;
                }
                
                translationCount++;
                
                if(translationCount > translationLimit){
                    //translationMap created an infinite loop
                    //something is wrong, we should not be looping more than translationLimit 
                    break;
                }
            }

            if(!keysToIgnoreSet.contains(key) && 
                    !( 
                       //key.endsWith(".addressAsString")
                       key.endsWith("._type")     
                       || key.endsWith(".deviceMacAddress.address")
                       || key.endsWith(".deviceMacAddress.addressAsString")
                       || key.endsWith(".macAddressStr")
                       || key.endsWith(".macAddress")
                       || key.endsWith(".bssid.address")
                       || key.endsWith(".bssid.addressAsString")
                       || key.endsWith(".equipmentId")
                       || key.endsWith(".radioType")
                       //
                       || "eventTimestamp".equals(key)
                       || "createdTimestamp".equals(key)
                       || "lastModifiedTimestamp".equals(key)
                     )
                            ){
                //skip constant and un-interesting values
                ret.put(commonTypePrefix + key, entry.getValue());
            }
            
        }
        
        //add timestamp column:
        Long ts = (Long)map.get("eventTimestamp");
        if(ts==null || ts.longValue()==0){
            ts = (Long)map.get("createdTimestamp");
            if(ts==null || ts.longValue()==0){
                ts = (Long)map.get("lastModifiedTimestamp");
            }
        }
        
        ret.put(DatasetFlattener.getTimestampColumnName(), ts);
        
//        System.out.println("******* translationMap = ");
//        for(Map.Entry<String, String> entry: translationMap.entrySet()){
//            System.out.format("'%s' => '%s' %n", entry.getKey(), entry.getValue());
//        }
        
        return ret;
    }
    
    private static void printMap(Map<String, Object> map){
        if(map == null){
            return;
        }
        
        for(Map.Entry<String, Object> entry: map.entrySet()){
            Object value = entry.getValue();
            System.out.println(entry.getKey()+ " => " + (value instanceof String?'"':"") + value + (value instanceof String?'"':""));
        }
    }
    
    
    private static final String jsonStrEquipmentEventCounts = 
    "{"+
    "    \"_type\": \"EquipmentEventCounts\","+
    "    \"id\": 0,"+
    "    \"createdTimestamp\": 1491935460000,"+
    "    \"lastModifiedTimestamp\": 1491935431090,"+
    "    \"data\": {"+
    "        \"_type\": \"EquipmentEventCounts\","+
    "        \"periodLengthSec\": 60,"+
    "        \"countsPerType\": {"+
    "            \"WIFI_ASSOC_REQ\": 2,"+
    "            \"WIFI_DISASSOC\": 2,"+
    "            \"WIFI_AUTH\": 4,"+
    "            \"WIFI_ASSOC_RESP\": 2,"+
    "            \"STA_Client_Id\": 2,"+
    "            \"STA_Client_Failure\": 2,"+
    "            \"STA_Client_Assoc\": 2,"+
    "            \"EAPOL_Event\": 17,"+
    "            \"STA_Client_Auth\": 2,"+
    "            \"STA_Client_Disconnect\": 2"+
    "        },"+
    "        \"type\": \"EquipmentEventCounts\""+
    "    },"+
    "    \"processingStartTime\": 0,"+
    "    \"customerId\": 13,"+
    "    \"equipmentId\": 82,"+
    "    \"dataType\": \"EquipmentEventCounts\","+
    "    \"createdTimestampStr\": \"2017-04-11 14:31:00\","+
    "    \"lastModifiedTimestampStr\": \"2017-04-11 14:30:31\","+
    "    \"qrCode\": \"dev-ap-0022\","+
    "    \"apName\": \"AP22\","+
    "    \"reportType\": \"Metric\","+
    "    \"eventTimestamp\": 1491935460000"+
    "}";
    

    private static final String jsonStrAPDemoMetric = 
            "{"+
                    "\"_type\": \"APDemoMetric\","+
                    "\"id\": 0,"+
                    "\"createdTimestamp\": 1491935686764,"+
                    "\"lastModifiedTimestamp\": 1491935687796,"+
                    "\"data\": {"+
                    "\"_type\": \"APDemoMetric\","+
                    "\"periodLengthSec\": 60,"+
                    "\"clientMacAddresses\": [],"+
                    "\"txBytes2G\": 235995,"+
                    "\"rxBytes2G\": 0,"+
                    "\"txBytes5G\": 15070761,"+
                    "\"rxBytes5G\": 30533643,"+
                    "\"noiseFloor2G\": -73,"+
                    "\"noiseFloor5G\": -91,"+
                    "\"tunnelMetrics\": [],"+
                    "\"networkProbeMetrics\": ["+
                    "{"+
                    "\"_type\": \"NetworkProbeMetrics\","+
                    "\"vlanIF\": \"brtrunk\","+
                    "\"dhcpState\": \"enabled\","+
                    "\"dhcpLatencyMs\": 337,"+
                    "\"dnsState\": \"enabled\","+
                    "\"dnsLatencyMs\": 9,"+
                    "\"radiusLatencyInMs\": 0"+
                    "}"+
                    "],"+
                    "\"radiusMetrics\": ["+
                    "{"+
                    "\"_type\": \"RadiusMetrics\","+
                    "\"serverIp\": \"172.16.10.244\","+
                    "\"numberOfNoAnswer\": 0,"+
                    "\"minLatencyMs\": 222,"+
                    "\"maxLatencyMs\": 512,"+
                    "\"avgLatencyMs\": 316"+
                    "}"+
                    "],"+
                    "\"cloudLinkAvailability\": 100,"+
                    "\"cloudLinkLatencyInMs\": 29,"+
                    "\"channelUtilization2G\": 99,"+
                    "\"channelUtilization5G\": 32,"+
                    "\"apPerformance\": {"+
                    "\"_type\": \"ApPerformance\","+
                    "\"freeMemory\": 184904,"+
                    "\"cpuUtilized\": \"Bw4=\","+
                    "\"upTime\": 21827,"+
                    "\"camiCrashed\": 0,"+
                    "\"cpuTemperature\": 81,"+
                    "\"ethLinkState\": \"UP1000_FULL_DUPLEX\","+
                    "\"cloudTxBytes\": 28543,"+
                    "\"cloudRxBytes\": 15720,"+
                    "\"cpuUtilizedStr\": \"[7,14]\""+
                    "},"+
                    "\"radioUtilization2G\": ["+
                    "{"+
                    "\"_type\": \"RadioUtilization\","+
                    "\"assocClientTx\": 0,"+
                    "\"unassocClientTx\": 4,"+
                    "\"assocClientRx\": 0,"+
                    "\"unassocClientRx\": 23,"+
                    "\"nonWifi\": 68"+
                    "}"+
                    "],"+
                    "\"radioUtilization5G\": ["+
                    "{"+
                    "\"_type\": \"RadioUtilization\","+
                    "\"assocClientTx\": 10,"+
                    "\"unassocClientTx\": 4,"+
                    "\"assocClientRx\": 3,"+
                    "\"unassocClientRx\": 15,"+
                    "\"nonWifi\": 1"+
                    "}"+
                    "],"+
                    "\"radioStats2G\": {"+
                    "\"_type\": \"RadioStatistics\","+
                    "\"numRadioResets\": 0,"+
                    "\"numChanChanges\": 0,"+
                    "\"numTxPowerChanges\": 0,"+
                    "\"numRadarChanChanges\": 0,"+
                    "\"numFreeTxBuf\": 507,"+
                    "\"elevenGProtection\": 0,"+
                    "\"numScanReq\": 0,"+
                    "\"numScanSucc\": 0,"+
                    "\"curEirp\": 18,"+
                    "\"actualCellSize\": ["+
                    "-76,"+
                    "-76,"+
                    "-76,"+
                    "-76,"+
                    "-76,"+
                    "-76,"+
                    "-76,"+
                    "-76,"+
                    "-76,"+
                    "-74,"+
                    "-74,"+
                    "-74,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-70,"+
                    "-70,"+
                    "-68,"+
                    "-68,"+
                    "-70,"+
                    "-70,"+
                    "-70,"+
                    "-70,"+
                    "-69,"+
                    "-69,"+
                    "-69,"+
                    "-69,"+
                    "-69,"+
                    "-69,"+
                    "-70,"+
                    "-70,"+
                    "-70,"+
                    "-70,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-72,"+
                    "-68,"+
                    "-68,"+
                    "-68,"+
                    "-68,"+
                    "-68,"+
                    "-68,"+
                    "-68,"+
                    "-68,"+
                    "-68,"+
                    "-68"+
                    "],"+
                    "\"rxLastRssi\": -55,"+
                    "\"numRx\": 16302,"+
                    "\"numRxData\": 4286,"+
                    "\"rxDataBytes\": 640617,"+
                    "\"numRxBeacon\": 10202,"+
                    "\"numTxSucc\": 1754,"+
                    "\"numTxBeaconSucc\": 0,"+
                    "\"numTxBeaconFail\": 0,"+
                    "\"numTxBeaconSuFail\": 0,"+
                    "\"numTxData\": 1659,"+
                    "\"numTxTotalAttemps\": 2324"+
                    "},"+
                    "\"radioStats5G\": {"+
                    "\"_type\": \"RadioStatistics\","+
                    "\"numRadioResets\": 0,"+
                    "\"numChanChanges\": 0,"+
                    "\"numTxPowerChanges\": 0,"+
                    "\"numRadarChanChanges\": 0,"+
                    "\"numFreeTxBuf\": 511,"+
                    "\"numScanReq\": 0,"+
                    "\"numScanSucc\": 0,"+
                    "\"curEirp\": 21,"+
                    "\"actualCellSize\": ["+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-86,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-87,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-84,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86,"+
                    "-86"+
                    "],"+
                    "\"rxLastRssi\": -48,"+
                    "\"numRx\": 62268,"+
                    "\"numRxData\": 49862,"+
                    "\"rxDataBytes\": 33585144,"+
                    "\"numRxBeacon\": 8900,"+
                    "\"numTxSucc\": 21752,"+
                    "\"numTxBeaconSucc\": 0,"+
                    "\"numTxBeaconFail\": 0,"+
                    "\"numTxBeaconSuFail\": 0,"+
                    "\"numTxData\": 21248,"+
                    "\"numTxTotalAttemps\": 29742"+
                    "},"+
                    "\"twoGWmmQueue\": {"+
                    "\"BE\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"BE\","+
                    "\"txFrames\": 1915,"+
                    "\"txBytes\": 235995,"+
                    "\"txFailedFrames\": 645,"+
                    "\"txFailedBytes\": 63790,"+
                    "\"rxFrames\": 0,"+
                    "\"rxBytes\": 0,"+
                    "\"rxFailedFrames\": 0,"+
                    "\"rxFailedBytes\": 0,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "},"+
                    "\"BK\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"BK\","+
                    "\"txFrames\": 0,"+
                    "\"txBytes\": 0,"+
                    "\"txFailedFrames\": 0,"+
                    "\"txFailedBytes\": 0,"+
                    "\"rxFrames\": 0,"+
                    "\"rxBytes\": 0,"+
                    "\"rxFailedFrames\": 0,"+
                    "\"rxFailedBytes\": 0,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "},"+
                    "\"VO\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"VO\","+
                    "\"txFrames\": 0,"+
                    "\"txBytes\": 0,"+
                    "\"txFailedFrames\": 0,"+
                    "\"txFailedBytes\": 0,"+
                    "\"rxFrames\": 0,"+
                    "\"rxBytes\": 0,"+
                    "\"rxFailedFrames\": 0,"+
                    "\"rxFailedBytes\": 0,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "},"+
                    "\"VI\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"VI\","+
                    "\"txFrames\": 0,"+
                    "\"txBytes\": 0,"+
                    "\"txFailedFrames\": 0,"+
                    "\"txFailedBytes\": 0,"+
                    "\"rxFrames\": 0,"+
                    "\"rxBytes\": 0,"+
                    "\"rxFailedFrames\": 0,"+
                    "\"rxFailedBytes\": 0,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "}"+
                    "},"+
                    "\"fiveGWmmQueue\": {"+
                    "\"BE\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"BE\","+
                    "\"txFrames\": 27294,"+
                    "\"txBytes\": 15070761,"+
                    "\"txFailedFrames\": 605,"+
                    "\"txFailedBytes\": 58220,"+
                    "\"rxFrames\": 114,"+
                    "\"rxBytes\": 14670,"+
                    "\"rxFailedFrames\": 48,"+
                    "\"rxFailedBytes\": 1306,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "},"+
                    "\"BK\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"BK\","+
                    "\"txFrames\": 0,"+
                    "\"txBytes\": 0,"+
                    "\"txFailedFrames\": 0,"+
                    "\"txFailedBytes\": 0,"+
                    "\"rxFrames\": 42312,"+
                    "\"rxBytes\": 30517148,"+
                    "\"rxFailedFrames\": 0,"+
                    "\"rxFailedBytes\": 0,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "},"+
                    "\"VO\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"VO\","+
                    "\"txFrames\": 0,"+
                    "\"txBytes\": 0,"+
                    "\"txFailedFrames\": 0,"+
                    "\"txFailedBytes\": 0,"+
                    "\"rxFrames\": 19,"+
                    "\"rxBytes\": 1825,"+
                    "\"rxFailedFrames\": 0,"+
                    "\"rxFailedBytes\": 0,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "},"+
                    "\"VI\": {"+
                    "\"_type\": \"WmmQueueStats\","+
                    "\"queueType\": \"VI\","+
                    "\"txFrames\": 0,"+
                    "\"txBytes\": 0,"+
                    "\"txFailedFrames\": 0,"+
                    "\"txFailedBytes\": 0,"+
                    "\"rxFrames\": 0,"+
                    "\"rxBytes\": 0,"+
                    "\"rxFailedFrames\": 0,"+
                    "\"rxFailedBytes\": 0,"+
                    "\"forwardFrames\": 0,"+
                    "\"forwardBytes\": 0,"+
                    "\"txExpiredFrames\": 0,"+
                    "\"txExpiredBytes\": 0"+
                    "}"+
                    "},"+
                    "\"clientCount\": 0,"+
                    "\"cellSize2G\": -68,"+
                    "\"minCellSize2G\": -68,"+
                    "\"cellSize5G\": -86,"+
                    "\"minCellSize5G\": -84,"+
                    "\"type\": \"APDemoMetric\","+
                    "\"clientMacAddressesStr\": []"+
                    "},"+
                    "\"processingStartTime\": 0,"+
                    "\"customerId\": 13,"+
                    "\"equipmentId\": 82,"+
                    "\"dataType\": \"APDemoMetric\","+
                    "\"createdTimestampStr\": \"2017-04-11 14:34:46\","+
                    "\"lastModifiedTimestampStr\": \"2017-04-11 14:34:47\","+
                    "\"qrCode\": \"dev-ap-0022\","+
                    "\"apName\": \"AP22\","+
                    "\"reportType\": \"Metric\","+
                    "\"eventTimestamp\": 1491935686764"+
                    "}"
                    ;        

    private static final String jsonStrApSsidMetrics = 
        "{"+
        "\"_type\": \"ApSsidMetrics\","+
        "\"id\": 0,"+
        "\"createdTimestamp\": 1491935686764,"+
        "\"lastModifiedTimestamp\": 1491935687799,"+
        "\"data\": {"+
        "\"_type\": \"ApSsidMetrics\","+
        "\"ssidStats2g\": ["+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KDCDevices\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLg\","+
        "\"addressAsString\": \"74:9c:e3:01:02:e0\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KodaCloud\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLh\","+
        "\"addressAsString\": \"74:9c:e3:01:02:e1\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"kdcbestap\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLi\","+
        "\"addressAsString\": \"74:9c:e3:01:02:e2\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KodaCloud_Guest\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLj\","+
        "\"addressAsString\": \"74:9c:e3:01:02:e3\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KodaWEP\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLk\","+
        "\"addressAsString\": \"74:9c:e3:01:02:e4\""+
        "},"+
        "\"numClient\": 0"+
        "}"+
        "],"+
        "\"ssidStats5g\": ["+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KDCDevices\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLw\","+
        "\"addressAsString\": \"74:9c:e3:01:02:f0\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KodaCloud\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLx\","+
        "\"addressAsString\": \"74:9c:e3:01:02:f1\""+
        "},"+
        "\"numClient\": 3"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"kdcbestap\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLy\","+
        "\"addressAsString\": \"74:9c:e3:01:02:f2\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KodaCloud_Guest\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQLz\","+
        "\"addressAsString\": \"74:9c:e3:01:02:f3\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KodaCloud Sales\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQL0\","+
        "\"addressAsString\": \"74:9c:e3:01:02:f4\""+
        "},"+
        "\"numClient\": 0"+
        "},"+
        "{"+
        "\"_type\": \"SsidStatistics\","+
        "\"ssid\": \"KodaWEP\","+
        "\"bssid\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"dJzjAQL1\","+
        "\"addressAsString\": \"74:9c:e3:01:02:f5\""+
        "},"+
        "\"numClient\": 0"+
        "}"+
        "],"+
        "\"type\": \"ApSsidMetrics\""+
        "},"+
        "\"processingStartTime\": 0,"+
        "\"customerId\": 13,"+
        "\"equipmentId\": 82,"+
        "\"dataType\": \"ApSsidMetrics\","+
        "\"createdTimestampStr\": \"2017-04-11 14:34:46\","+
        "\"lastModifiedTimestampStr\": \"2017-04-11 14:34:47\","+
        "\"qrCode\": \"dev-ap-0022\","+
        "\"apName\": \"AP22\","+
        "\"reportType\": \"Metric\","+
        "\"eventTimestamp\": 1491935686764"+
        "}"
    ;
    
    private static final String jsonStrApClientMetrics = 
        "{"+
        "\"_type\": \"ApClientMetrics\","+
        "\"id\": 0,"+
        "\"createdTimestamp\": 1491935686989,"+
        "\"lastModifiedTimestamp\": 1491935687840,"+
        "\"data\": {"+
        "\"_type\": \"ApClientMetrics\","+
        "\"clientMetrics5g\": ["+
        "{"+
        "\"_type\": \"ClientMetrics\","+
        "\"macAddress\": \"sHAtiso0\","+
        "\"deviceMacAddress\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"sHAtiso0\","+
        "\"addressAsString\": \"b0:70:2d:8a:ca:34\""+
        "},"+
        "\"secondsSinceLastRecv\": 13,"+
        "\"numRxPackets\": 256,"+
        "\"numTxPackets\": 226,"+
        "\"numRxBytes\": 20812,"+
        "\"numTxBytes\": 79539,"+
        "\"txRetries\": 257,"+
        "\"rxDuplicatePackets\": 0,"+
        "\"rateCount\": 8,"+
        "\"rates\": \"DBIYJDBIYGwAAAAAAAAAAA==\","+
        "\"mcs\": \"/wAAAAAAAAAAAAAAAAAAAA==\","+
        "\"vhtMcs\": 65530,"+
        "\"snr\": 26,"+
        "\"rssi\": -66,"+
        "\"sessionId\": 3797147718590907,"+
        "\"rxLastRssi\": -66,"+
        "\"numRxData\": 61,"+
        "\"rxBytes\": 4117,"+
        "\"rxDataBytes\": 3729,"+
        "\"lastRecvLayer3Ts\": 1491935685459,"+
        "\"numTxSucc\": 37,"+
        "\"numTxByteSucc\": 12730,"+
        "\"numTxData\": 37,"+
        "\"numTxDataRetries\": 28,"+
        "\"lastSentLayer3Ts\": 1491935683491,"+
        "\"lastRxMcsIdx\": \"MCS_AC_2x2_6\","+
        "\"lastTxMcsIdx\": \"MCS_AC_2x2_8\","+
        "\"radioType\": \"is5GHz\","+
        "\"lastRxPhyRateKb\": 243000,"+
        "\"lastTxPhyRateKb\": 324000,"+
        "\"macAddressStr\": \"B0:70:2D:8A:CA:34\","+
        "\"ratesStr\": \"[12,18,24,36,48,72,96,108,0,0,0,0,0,0,0,0]\","+
        "\"mcsStr\": \"[255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]\""+
        "},"+
        "{"+
        "\"_type\": \"ClientMetrics\","+
        "\"macAddress\": \"gOZQHnBA\","+
        "\"deviceMacAddress\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"gOZQHnBA\","+
        "\"addressAsString\": \"80:e6:50:1e:70:40\""+
        "},"+
        "\"secondsSinceLastRecv\": 2,"+
        "\"numRxPackets\": 6650114,"+
        "\"numTxPackets\": 5611827,"+
        "\"numRxBytes\": 3752361465,"+
        "\"numTxBytes\": 1676497648,"+
        "\"txRetries\": 1305,"+
        "\"rxDuplicatePackets\": 0,"+
        "\"rateCount\": 8,"+
        "\"rates\": \"DBIYJDBIYGwAAAAAAAAAAA==\","+
        "\"mcs\": \"//8AAAAAAAAAAAAAAAAAAA==\","+
        "\"vhtMcs\": 65530,"+
        "\"snr\": 43,"+
        "\"rssi\": -49,"+
        "\"sessionId\": 4013270472923262,"+
        "\"rxLastRssi\": -49,"+
        "\"numRxData\": 9362,"+
        "\"rxBytes\": 5746855,"+
        "\"rxDataBytes\": 5746855,"+
        "\"lastRecvLayer3Ts\": 1491935686349,"+
        "\"numTxSucc\": 5595,"+
        "\"numTxByteSucc\": 3584678,"+
        "\"numTxData\": 4183,"+
        "\"numTxDataRetries\": 645,"+
        "\"lastSentLayer3Ts\": 1491935686348,"+
        "\"lastRxMcsIdx\": \"MCS_AC_2x2_9\","+
        "\"lastTxMcsIdx\": \"MCS_AC_2x2_9\","+
        "\"radioType\": \"is5GHz\","+
        "\"lastRxPhyRateKb\": 360000,"+
        "\"lastTxPhyRateKb\": 360000,"+
        "\"macAddressStr\": \"80:E6:50:1E:70:40\","+
        "\"ratesStr\": \"[12,18,24,36,48,72,96,108,0,0,0,0,0,0,0,0]\","+
        "\"mcsStr\": \"[255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0]\""+
        "},"+
        "{"+
        "\"_type\": \"ClientMetrics\","+
        "\"macAddress\": \"vFQ2PVn4\","+
        "\"deviceMacAddress\": {"+
        "\"_type\": \"MacAddress\","+
        "\"address\": \"vFQ2PVn4\","+
        "\"addressAsString\": \"bc:54:36:3d:59:f8\""+
        "},"+
        "\"secondsSinceLastRecv\": 106,"+
        "\"numRxPackets\": 226545,"+
        "\"numTxPackets\": 209775,"+
        "\"numRxBytes\": 40597860,"+
        "\"numTxBytes\": 198736215,"+
        "\"txRetries\": 1813,"+
        "\"rxDuplicatePackets\": 15,"+
        "\"rateCount\": 8,"+
        "\"rates\": \"DBIYJDBIYGwAAAAAAAAAAA==\","+
        "\"mcs\": \"//8AAAAAAAAAAAAAAAAAAA==\","+
        "\"vhtMcs\": 65530,"+
        "\"snr\": 26,"+
        "\"rssi\": -66,"+
        "\"sessionId\": 144729004903164,"+
        "\"rxLastRssi\": -66,"+
        "\"numRxData\": 2,"+
        "\"rxBytes\": 0,"+
        "\"rxDataBytes\": 0,"+
        "\"lastRecvLayer3Ts\": 1491935673528,"+
        "\"numTxSucc\": 0,"+
        "\"numTxByteSucc\": 0,"+
        "\"numTxData\": 0,"+
        "\"numTxDataRetries\": 0,"+
        "\"lastSentLayer3Ts\": 1491935673504,"+
        "\"lastRxMcsIdx\": \"MCS_AC_1x1_4\","+
        "\"lastTxMcsIdx\": \"MCS_AC_2x2_6\","+
        "\"radioType\": \"is5GHz\","+
        "\"lastRxPhyRateKb\": 81000,"+
        "\"lastTxPhyRateKb\": 243000,"+
        "\"macAddressStr\": \"BC:54:36:3D:59:F8\","+
        "\"ratesStr\": \"[12,18,24,36,48,72,96,108,0,0,0,0,0,0,0,0]\","+
        "\"mcsStr\": \"[255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0]\""+
        "}"+
        "],"+
        "\"periodLengthSec\": 15,"+
        "\"clientCount2g\": 0,"+
        "\"clientCount5g\": 3,"+
        "\"type\": \"ApClientMetrics\""+
        "},"+
        "\"processingStartTime\": 0,"+
        "\"customerId\": 13,"+
        "\"equipmentId\": 82,"+
        "\"dataType\": \"ApClientMetrics\","+
        "\"createdTimestampStr\": \"2017-04-11 14:34:46\","+
        "\"lastModifiedTimestampStr\": \"2017-04-11 14:34:47\","+
        "\"qrCode\": \"dev-ap-0022\","+
        "\"apName\": \"AP22\","+
        "\"reportType\": \"Metric\","+
        "\"eventTimestamp\": 1491935686989"+
        "}"
            ;
            
    private static String jsonEquipmentScanEvent = 
        "{"+
        "\"_type\": \"EquipmentScanEvent\","+
        "\"eventTimestamp\": 1502291569348,"+
        "\"deploymentId\": \"1\","+
        "\"customerId\": 383,"+
        "\"queueName\": null,"+
        "\"equipmentId\": 3380,"+
        "\"equipmentScanRecord\": {"+
        "\"_type\": \"EquipmentScanRecord\","+
        "\"id\": 0,"+
        "\"equipmentId\": 3380,"+
        "\"customerId\": 383,"+
        "\"scanDetails\": {"+
        "\"_type\": \"EquipmentScanDetails\","+
        "\"managedNeighbours\": {"+
        "\"383\": ["+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 968,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTA\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 21,"+
        "\"signal\": -76,"+
        "\"scanTimeInSeconds\": 1502291389,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTG\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 21,"+
        "\"signal\": -76,"+
        "\"scanTimeInSeconds\": 1502291385,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTE\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 21,"+
        "\"signal\": -76,"+
        "\"scanTimeInSeconds\": 1502291389,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTC\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 21,"+
        "\"signal\": -76,"+
        "\"scanTimeInSeconds\": 1502291389,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTF\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 19,"+
        "\"signal\": -77,"+
        "\"scanTimeInSeconds\": 1502291389,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTB\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 22,"+
        "\"signal\": -75,"+
        "\"scanTimeInSeconds\": 1502291389,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTU\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 22,"+
        "\"signal\": -75,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTV\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 22,"+
        "\"signal\": -75,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTW\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 22,"+
        "\"signal\": -75,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTS\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 22,"+
        "\"signal\": -75,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTQ\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 22,"+
        "\"signal\": -75,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVTR\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 22,"+
        "\"signal\": -75,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 941,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 6,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFh\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 6,"+
        "\"rate\": 0,"+
        "\"rssi\": 29,"+
        "\"signal\": -70,"+
        "\"scanTimeInSeconds\": 1502291329,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFg\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 6,"+
        "\"rate\": 0,"+
        "\"rssi\": 29,"+
        "\"signal\": -70,"+
        "\"scanTimeInSeconds\": 1502291329,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFk\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 6,"+
        "\"rate\": 0,"+
        "\"rssi\": 29,"+
        "\"signal\": -70,"+
        "\"scanTimeInSeconds\": 1502291330,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFl\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 6,"+
        "\"rate\": 0,"+
        "\"rssi\": 29,"+
        "\"signal\": -70,"+
        "\"scanTimeInSeconds\": 1502291330,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFi\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 6,"+
        "\"rate\": 0,"+
        "\"rssi\": 28,"+
        "\"signal\": -71,"+
        "\"scanTimeInSeconds\": 1502291330,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFm\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 6,"+
        "\"rate\": 0,"+
        "\"rssi\": 28,"+
        "\"signal\": -71,"+
        "\"scanTimeInSeconds\": 1502291330,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFy\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 47,"+
        "\"signal\": -57,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVF1\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 46,"+
        "\"signal\": -58,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFw\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 47,"+
        "\"signal\": -57,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVFx\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 47,"+
        "\"signal\": -57,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVF2\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 47,"+
        "\"signal\": -57,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVF0\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 149,"+
        "\"rate\": 0,"+
        "\"rssi\": 46,"+
        "\"signal\": -58,"+
        "\"scanTimeInSeconds\": 1502291310,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 646,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfA\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 36,"+
        "\"signal\": -65,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfB\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 36,"+
        "\"signal\": -65,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfC\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 36,"+
        "\"signal\": -65,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfG\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 33,"+
        "\"signal\": -67,"+
        "\"scanTimeInSeconds\": 1502291268,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfE\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 33,"+
        "\"signal\": -67,"+
        "\"scanTimeInSeconds\": 1502291268,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfF\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 35,"+
        "\"signal\": -66,"+
        "\"scanTimeInSeconds\": 1502291401,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 157,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfQ\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 157,"+
        "\"rate\": 0,"+
        "\"rssi\": 49,"+
        "\"signal\": -56,"+
        "\"scanTimeInSeconds\": 1502291331,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfR\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 157,"+
        "\"rate\": 0,"+
        "\"rssi\": 49,"+
        "\"signal\": -56,"+
        "\"scanTimeInSeconds\": 1502291331,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfT\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 157,"+
        "\"rate\": 0,"+
        "\"rssi\": 49,"+
        "\"signal\": -56,"+
        "\"scanTimeInSeconds\": 1502291331,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfW\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 157,"+
        "\"rate\": 0,"+
        "\"rssi\": 49,"+
        "\"signal\": -56,"+
        "\"scanTimeInSeconds\": 1502291331,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfV\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 157,"+
        "\"rate\": 0,"+
        "\"rssi\": 49,"+
        "\"signal\": -56,"+
        "\"scanTimeInSeconds\": 1502291331,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVfS\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 157,"+
        "\"rate\": 0,"+
        "\"rssi\": 49,"+
        "\"signal\": -56,"+
        "\"scanTimeInSeconds\": 1502291331,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "}"+
        "]"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 703,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHi\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 40,"+
        "\"signal\": -62,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHh\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 40,"+
        "\"signal\": -62,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHl\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 40,"+
        "\"signal\": -62,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHm\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 40,"+
        "\"signal\": -62,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHg\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 40,"+
        "\"signal\": -62,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHj\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 40,"+
        "\"signal\": -62,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVH2\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 46,"+
        "\"signal\": -58,"+
        "\"scanTimeInSeconds\": 1502291075,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVH0\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 46,"+
        "\"signal\": -58,"+
        "\"scanTimeInSeconds\": 1502291075,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVH1\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 46,"+
        "\"signal\": -58,"+
        "\"scanTimeInSeconds\": 1502291075,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHw\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 45,"+
        "\"signal\": -59,"+
        "\"scanTimeInSeconds\": 1502291361,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHx\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 45,"+
        "\"signal\": -59,"+
        "\"scanTimeInSeconds\": 1502291361,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVHy\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 43,"+
        "\"signal\": -60,"+
        "\"scanTimeInSeconds\": 1502291361,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "}"+
        "]"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 689,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRG\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 12,"+
        "\"signal\": -82,"+
        "\"scanTimeInSeconds\": 1502287970,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRC\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 8,"+
        "\"signal\": -85,"+
        "\"scanTimeInSeconds\": 1502288584,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRS\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 21,"+
        "\"signal\": -76,"+
        "\"scanTimeInSeconds\": 1502289489,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRR\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 21,"+
        "\"signal\": -76,"+
        "\"scanTimeInSeconds\": 1502289489,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRU\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 19,"+
        "\"signal\": -77,"+
        "\"scanTimeInSeconds\": 1502290604,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRV\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 21,"+
        "\"signal\": -76,"+
        "\"scanTimeInSeconds\": 1502290604,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRW\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 19,"+
        "\"signal\": -77,"+
        "\"scanTimeInSeconds\": 1502290604,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVRT\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 19,"+
        "\"signal\": -77,"+
        "\"scanTimeInSeconds\": 1502290604,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 4505,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKk\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 15,"+
        "\"signal\": -80,"+
        "\"scanTimeInSeconds\": 1502288717,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKm\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 15,"+
        "\"signal\": -80,"+
        "\"scanTimeInSeconds\": 1502289087,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKg\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 15,"+
        "\"signal\": -80,"+
        "\"scanTimeInSeconds\": 1502289087,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKi\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 14,"+
        "\"signal\": -81,"+
        "\"scanTimeInSeconds\": 1502289087,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKl\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 15,"+
        "\"signal\": -80,"+
        "\"scanTimeInSeconds\": 1502289087,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKh\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 14,"+
        "\"signal\": -81,"+
        "\"scanTimeInSeconds\": 1502289619,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 36,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQK2\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 36,"+
        "\"rate\": 0,"+
        "\"rssi\": 8,"+
        "\"signal\": -85,"+
        "\"scanTimeInSeconds\": 1502284087,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKy\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 36,"+
        "\"rate\": 0,"+
        "\"rssi\": 8,"+
        "\"signal\": -85,"+
        "\"scanTimeInSeconds\": 1502285512,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQK1\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 36,"+
        "\"rate\": 0,"+
        "\"rssi\": 7,"+
        "\"signal\": -86,"+
        "\"scanTimeInSeconds\": 1502286169,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQK0\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 36,"+
        "\"rate\": 0,"+
        "\"rssi\": 9,"+
        "\"signal\": -84,"+
        "\"scanTimeInSeconds\": 1502288925,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKx\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 36,"+
        "\"rate\": 0,"+
        "\"rssi\": 8,"+
        "\"signal\": -85,"+
        "\"scanTimeInSeconds\": 1502288927,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQKw\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 36,"+
        "\"rate\": 0,"+
        "\"rssi\": 8,"+
        "\"signal\": -85,"+
        "\"scanTimeInSeconds\": 1502288926,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "}"+
        "]"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 4532,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQJA\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 11,"+
        "\"signal\": -83,"+
        "\"scanTimeInSeconds\": 1502291391,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQJG\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 12,"+
        "\"signal\": -82,"+
        "\"scanTimeInSeconds\": 1502291391,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQJE\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 11,"+
        "\"signal\": -83,"+
        "\"scanTimeInSeconds\": 1502291391,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQJF\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 12,"+
        "\"signal\": -82,"+
        "\"scanTimeInSeconds\": 1502291391,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQJC\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 9,"+
        "\"signal\": -84,"+
        "\"scanTimeInSeconds\": 1502291391,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjBQJB\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 11,"+
        "\"signal\": -83,"+
        "\"scanTimeInSeconds\": 1502291391,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": null"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 827,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaB\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 1,"+
        "\"signal\": -90,"+
        "\"scanTimeInSeconds\": 1502290778,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaD\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 2,"+
        "\"signal\": -89,"+
        "\"scanTimeInSeconds\": 1502291032,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaC\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 1,"+
        "\"signal\": -90,"+
        "\"scanTimeInSeconds\": 1502291137,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaG\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 2,"+
        "\"signal\": -89,"+
        "\"scanTimeInSeconds\": 1502291125,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaE\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 36,"+
        "\"signal\": -65,"+
        "\"scanTimeInSeconds\": 1502291247,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaA\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 5,"+
        "\"signal\": -87,"+
        "\"scanTimeInSeconds\": 1502291357,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaS\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 11,"+
        "\"signal\": -83,"+
        "\"scanTimeInSeconds\": 1502288047,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaU\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 9,"+
        "\"signal\": -84,"+
        "\"scanTimeInSeconds\": 1502288047,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaW\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 12,"+
        "\"signal\": -82,"+
        "\"scanTimeInSeconds\": 1502288047,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaT\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 11,"+
        "\"signal\": -83,"+
        "\"scanTimeInSeconds\": 1502288047,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaR\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 11,"+
        "\"signal\": -83,"+
        "\"scanTimeInSeconds\": 1502290554,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAVaQ\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 12,"+
        "\"signal\": -82,"+
        "\"scanTimeInSeconds\": 1502291075,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"ManagedNeighbourEquipmentInfo\","+
        "\"equipmentId\": 683,"+
        "\"radioInfo2g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuE\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 30,"+
        "\"signal\": -69,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuC\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 32,"+
        "\"signal\": -68,"+
        "\"scanTimeInSeconds\": 1502291155,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuG\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 28,"+
        "\"signal\": -71,"+
        "\"scanTimeInSeconds\": 1502291268,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuB\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 28,"+
        "\"signal\": -71,"+
        "\"scanTimeInSeconds\": 1502291268,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuD\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 26,"+
        "\"signal\": -72,"+
        "\"scanTimeInSeconds\": 1502291268,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuA\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 1,"+
        "\"rate\": 0,"+
        "\"rssi\": 26,"+
        "\"signal\": -72,"+
        "\"scanTimeInSeconds\": 1502291401,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "},"+
        "\"radioInfo5g\": {"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 153,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuS\","+
        "\"ssid\": \"northgate\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 153,"+
        "\"rate\": 0,"+
        "\"rssi\": 30,"+
        "\"signal\": -69,"+
        "\"scanTimeInSeconds\": 1502290788,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuV\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 153,"+
        "\"rate\": 0,"+
        "\"rssi\": 32,"+
        "\"signal\": -68,"+
        "\"scanTimeInSeconds\": 1502290788,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuU\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 153,"+
        "\"rate\": 0,"+
        "\"rssi\": 32,"+
        "\"signal\": -68,"+
        "\"scanTimeInSeconds\": 1502290788,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WEP\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuT\","+
        "\"ssid\": \"StoreOrdering\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 153,"+
        "\"rate\": 0,"+
        "\"rssi\": 30,"+
        "\"signal\": -69,"+
        "\"scanTimeInSeconds\": 1502290788,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuW\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 153,"+
        "\"rate\": 0,"+
        "\"rssi\": 30,"+
        "\"signal\": -69,"+
        "\"scanTimeInSeconds\": 1502290788,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"dJzjAUuR\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 153,"+
        "\"rate\": 0,"+
        "\"rssi\": 32,"+
        "\"signal\": -68,"+
        "\"scanTimeInSeconds\": 1502290788,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "}"+
        "}"+
        "]"+
        "},"+
        "\"unmanagedNeighbours\": ["+
        "{"+
        "\"_type\": \"UnmanagedNeighbourEquipmentInfo\","+
        "\"radios\": ["+
        "{"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"YvGJiUyh\","+
        "\"ssid\": \"Samsung Galaxy S7 edge 4570\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 32,"+
        "\"signal\": -68,"+
        "\"scanTimeInSeconds\": 1502291391,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "}"+
        "]"+
        "},"+
        "{"+
        "\"_type\": \"UnmanagedNeighbourEquipmentInfo\","+
        "\"radios\": ["+
        "{"+
        "\"_type\": \"NeighbourRadioInfo\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"bssIds\": ["+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"wFYnn4AR\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 60,"+
        "\"signal\": -48,"+
        "\"scanTimeInSeconds\": 1502290870,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourBssidInfo\","+
        "\"macAddress\": \"wFYnn4AP\","+
        "\"ssid\": \"GCS0L1036023\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 52,"+
        "\"signal\": -54,"+
        "\"scanTimeInSeconds\": 1502291413,"+
        "\"nMode\": null,"+
        "\"acMode\": null,"+
        "\"bMode\": null,"+
        "\"scanPacketType\": \"BEACON\","+
        "\"detectedAuthMode\": \"WPA\""+
        "}"+
        "]"+
        "}"+
        "]"+
        "}"+
        "],"+
        "\"reportType\": \"NeighbourScan\""+
        "},"+
        "\"createdTimestamp\": 0,"+
        "\"lastModifiedTimestamp\": 1502291569735"+
        "},"+
        "\"eventTimestampStr\": \"2017-08-09 11:12:49\","+
        "\"qrCode\": \"7X47IH9WDM7J\","+
        "\"apName\": \"NG-002-AP254F\","+
        "\"reportType\": \"SystemEvent\""+
        "}            "
        ;
            

    private static String jsonNeighbourScanReports =
        "{"+
        "\"_type\": \"NeighbourScanReports\","+
        "\"id\": 0,"+
        "\"createdTimestamp\": 1502291686437,"+
        "\"lastModifiedTimestamp\": 1502291700754,"+
        "\"data\": {"+
        "\"_type\": \"NeighbourScanReports\","+
        "\"neighbourReports\": ["+
        "{"+
        "\"_type\": \"NeighbourReport\","+
        "\"macAddress\": \"YvGJiUyh\","+
        "\"ssid\": \"Samsung Galaxy S7 edge 4570\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"channel\": 11,"+
        "\"rate\": 0,"+
        "\"rssi\": 30,"+
        "\"signal\": -69,"+
        "\"scanTimeInSeconds\": 1502291626,"+
        "\"packetType\": \"BEACON\","+
        "\"secureMode\": \"WPA\","+
        "\"macAddressStr\": \"62:F1:89:89:4C:A1\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourReport\","+
        "\"macAddress\": \"dJzjAVHy\","+
        "\"ssid\": \"WiFi10\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 45,"+
        "\"signal\": -59,"+
        "\"scanTimeInSeconds\": 1502291628,"+
        "\"packetType\": \"BEACON\","+
        "\"secureMode\": \"WEP\","+
        "\"macAddressStr\": \"74:9C:E3:01:51:F2\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourReport\","+
        "\"macAddress\": \"dJzjAVHz\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 45,"+
        "\"signal\": -59,"+
        "\"scanTimeInSeconds\": 1502291628,"+
        "\"packetType\": \"BEACON\","+
        "\"secureMode\": \"WPA\","+
        "\"macAddressStr\": \"74:9C:E3:01:51:F3\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourReport\","+
        "\"macAddress\": \"dJzjAVH0\","+
        "\"ssid\": \"(Non Broadcasting)\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 45,"+
        "\"signal\": -59,"+
        "\"scanTimeInSeconds\": 1502291628,"+
        "\"packetType\": \"BEACON\","+
        "\"secureMode\": \"WPA\","+
        "\"macAddressStr\": \"74:9C:E3:01:51:F4\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourReport\","+
        "\"macAddress\": \"dJzjAVH1\","+
        "\"ssid\": \"StoreINETOnly\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 45,"+
        "\"signal\": -59,"+
        "\"scanTimeInSeconds\": 1502291628,"+
        "\"packetType\": \"BEACON\","+
        "\"secureMode\": \"WPA\","+
        "\"macAddressStr\": \"74:9C:E3:01:51:F5\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourReport\","+
        "\"macAddress\": \"dJzjAVH2\","+
        "\"ssid\": \"StoreOrderingWPA2\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 161,"+
        "\"rate\": 0,"+
        "\"rssi\": 45,"+
        "\"signal\": -59,"+
        "\"scanTimeInSeconds\": 1502291628,"+
        "\"packetType\": \"BEACON\","+
        "\"secureMode\": \"WPA\","+
        "\"macAddressStr\": \"74:9C:E3:01:51:F6\""+
        "},"+
        "{"+
        "\"_type\": \"NeighbourReport\","+
        "\"macAddress\": \"wFYnn4AP\","+
        "\"ssid\": \"GCS0L1036023\","+
        "\"beaconInterval\": 0,"+
        "\"networkType\": \"AP\","+
        "\"privacy\": \"on\","+
        "\"radioType\": \"is5GHz\","+
        "\"channel\": 48,"+
        "\"rate\": 0,"+
        "\"rssi\": 52,"+
        "\"signal\": -54,"+
        "\"scanTimeInSeconds\": 1502291413,"+
        "\"packetType\": \"BEACON\","+
        "\"secureMode\": \"WPA\","+
        "\"macAddressStr\": \"C0:56:27:9F:80:0F\""+
        "}"+
        "],"+
        "\"type\": \"NeighbourScanReports\""+
        "},"+
        "\"processingStartTime\": 0,"+
        "\"deploymentId\": \"1\","+
        "\"customerId\": 383,"+
        "\"equipmentId\": 3380,"+
        "\"dataType\": \"NeighbourScanReports\","+
        "\"createdTimestampStr\": \"2017-08-09 11:14:46\","+
        "\"lastModifiedTimestampStr\": \"2017-08-09 11:15:00\","+
        "\"qrCode\": \"7X47IH9WDM7J\","+
        "\"apName\": \"NG-002-AP254F\","+
        "\"reportType\": \"Metric\","+
        "\"eventTimestamp\": 1502291686437"+
        "}            "
            ;
    
    private static String jsonChannelInfoReports = 
        "{"+
        "\"_type\": \"ChannelInfoReports\","+
        "\"id\": 0,"+
        "\"createdTimestamp\": 1502291624645,"+
        "\"lastModifiedTimestamp\": 1502291640910,"+
        "\"data\": {"+
        "\"_type\": \"ChannelInfoReports\","+
        "\"channelInformationReports2g\": ["+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 1,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 51,"+
        "\"wifiUtilization\": 38,"+
        "\"noiseFloor\": -82"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 2,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 42,"+
        "\"wifiUtilization\": 14,"+
        "\"noiseFloor\": -83"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 3,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 17,"+
        "\"wifiUtilization\": 1,"+
        "\"noiseFloor\": -82"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 4,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 2,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -83"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 5,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 11,"+
        "\"wifiUtilization\": 2,"+
        "\"noiseFloor\": -85"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 6,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 9,"+
        "\"wifiUtilization\": 6,"+
        "\"noiseFloor\": -84"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 7,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 4,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -82"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 8,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 9,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -84"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 9,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 6,"+
        "\"wifiUtilization\": 1,"+
        "\"noiseFloor\": -82"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 10,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 16,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -83"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 11,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 27,"+
        "\"wifiUtilization\": 19,"+
        "\"noiseFloor\": -83"+
        "}"+
        "],"+
        "\"channelInformationReports5g\": ["+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 36,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 40,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 2,"+
        "\"wifiUtilization\": 1,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 44,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 3,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 48,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 52,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 56,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 60,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 64,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 100,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 104,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 108,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 112,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 116,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 120,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 124,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 128,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 132,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 136,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 140,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 144,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 149,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 15,"+
        "\"wifiUtilization\": 8,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 153,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 0,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 157,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 13,"+
        "\"wifiUtilization\": 8,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 161,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 4,"+
        "\"wifiUtilization\": 4,"+
        "\"noiseFloor\": -89"+
        "},"+
        "{"+
        "\"_type\": \"ChannelInfo\","+
        "\"chanNumber\": 165,"+
        "\"bandwidth\": \"is20MHz\","+
        "\"totalUtilization\": 1,"+
        "\"wifiUtilization\": 0,"+
        "\"noiseFloor\": -89"+
        "}"+
        "],"+
        "\"type\": \"ChannelInfoReports\""+
        "},"+
        "\"processingStartTime\": 0,"+
        "\"deploymentId\": \"1\","+
        "\"customerId\": 383,"+
        "\"equipmentId\": 3380,"+
        "\"dataType\": \"ChannelInfoReports\","+
        "\"createdTimestampStr\": \"2017-08-09 11:13:44\","+
        "\"lastModifiedTimestampStr\": \"2017-08-09 11:14:00\","+
        "\"qrCode\": \"7X47IH9WDM7J\","+
        "\"apName\": \"NG-002-AP254F\","+
        "\"reportType\": \"Metric\","+
        "\"eventTimestamp\": 1502291624645"+
        "}"
            ;

    private static final String jsonCeBestApConfigUpdatedEvent = 
        "{"+
        "\"_type\": \"CeBestApConfigUpdatedEvent\","+
        "\"eventTimestamp\": 1502897408620,"+
        "\"deploymentId\": \"1\","+
        "\"customerId\": 383,"+
        "\"queueName\": null,"+
        "\"equipmentId\": 3380,"+
        "\"settings\": ["+
        "{"+
        "\"_type\": \"RadioBestApSettingsPerRadio\","+
        "\"radioType\": \"is2dot4GHz\","+
        "\"settings\": {"+
        "\"_type\": \"RadioBestApSettings\","+
        "\"mlComputed\": true,"+
        "\"dropInSnrPercentage\": 39,"+
        "\"minLoadFactor\": null"+
        "}"+
        "},"+
        "{"+
        "\"_type\": \"RadioBestApSettingsPerRadio\","+
        "\"radioType\": \"is5GHz\","+
        "\"settings\": {"+
        "\"_type\": \"RadioBestApSettings\","+
        "\"mlComputed\": true,"+
        "\"dropInSnrPercentage\": 24,"+
        "\"minLoadFactor\": null"+
        "}"+
        "}"+
        "],"+
        "\"eventTimestampStr\": \"2017-08-16 11:30:08\","+
        "\"qrCode\": \"7X47IH9WDM7J\","+
        "\"apName\": \"NG-002-AP254F\","+
        "\"reportType\": \"SystemEvent\""+
        "}"
    ;    
    
    public static void main(String[] args) {
        
        List<Pattern> pathPatternsToInclude = new ArrayList<>();
        List<Pattern> pathPatternsToExclude = new ArrayList<>();
        boolean splitMacAddressesIntoBytes = false;
        String jsonStr = "{}";
        printMap(flattenJson(jsonStr, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));
        
        System.out.println("******************************************************");

        pathPatternsToInclude.clear();
        pathPatternsToInclude.add(Pattern.compile("^_type$"));
        pathPatternsToInclude.add(Pattern.compile("^equipmentId$"));
        pathPatternsToInclude.add(Pattern.compile("^eventTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\..+STA_Client_Failure$"));

        printMap(flattenJson(jsonStrEquipmentEventCounts, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));
        
        System.out.println("******************************************************");
        
        // copy json from the history tab,
        // in eclipse select JSON lines, then replace string '"' with '\"' then replace regexp '^\s+(.+)' with '        \"\1\"\+' 

        pathPatternsToInclude.clear();
        pathPatternsToInclude.add(Pattern.compile("^_type$"));
        pathPatternsToInclude.add(Pattern.compile("^equipmentId$"));
        pathPatternsToInclude.add(Pattern.compile("^createdTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.apPerformance.+cpuUtilized$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numRxBeacon$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numRxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numTxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.minCellSize[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.noiseFloor[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.cellSize[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.channelUtilization[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.apPerformance\\.cpuTemperature$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioUtilization[25]G.+$"));
        
        //pathPatternsToInclude.clear();
        printMap(flattenJson(jsonStrAPDemoMetric, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));

        System.out.println("******************************************************");

        pathPatternsToInclude.clear();
        pathPatternsToInclude.add(Pattern.compile("^_type$"));
        pathPatternsToInclude.add(Pattern.compile("^equipmentId$"));
        pathPatternsToInclude.add(Pattern.compile("^createdTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.ssidStats[25]g.+addressAsString$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.ssidStats[25]g.+numClient$"));
        
        printMap(flattenJson(jsonStrApSsidMetrics, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));

        System.out.println("******************************************************");
        
        pathPatternsToInclude.clear();        
        pathPatternsToInclude.add(Pattern.compile("^_type$"));
        pathPatternsToInclude.add(Pattern.compile("^equipmentId$"));
        pathPatternsToInclude.add(Pattern.compile("^createdTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+addressAsString$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+txRetries$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxSucc$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxByteSucc$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxDataRetries$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+lastTxPhyRateKb$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numRxBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxDataBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxLastRssi$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+snr$"));
        
        printMap(flattenJson(jsonStrApClientMetrics, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));

        System.out.println("******************************************************");

//        pathPatternsToInclude.clear();
//        printMap(flattenJson(jsonEquipmentScanEvent, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));
//
//        System.out.println("******************************************************");

//        pathPatternsToInclude.clear();
//        printMap(flattenJson(jsonNeighbourScanReports, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));
//
//        System.out.println("******************************************************");

//        pathPatternsToInclude.clear();
//        printMap(flattenJson(jsonChannelInfoReports, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));
//
//        System.out.println("******************************************************");

        pathPatternsToInclude.clear();
        pathPatternsToInclude.add(Pattern.compile("^_type$"));
        pathPatternsToInclude.add(Pattern.compile("^equipmentId$"));
        pathPatternsToInclude.add(Pattern.compile("^eventTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^settings\\[\\d+\\]\\.radioType$"));
        pathPatternsToInclude.add(Pattern.compile("^settings\\[\\d+\\]\\.settings\\.dropInSnrPercentage$"));
        printMap(flattenJson(jsonCeBestApConfigUpdatedEvent, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes));

        System.out.println("******************************************************");
        
    }
}
