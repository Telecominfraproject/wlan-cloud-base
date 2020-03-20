package com.telecominfraproject.wlan.hierarchical.datastore.index;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * This class represents a directory index that is built from record index files. 
 * @author dtop
 *
 */
public class DirectoryIndex extends BaseJsonModel{

    private static final long serialVersionUID = -2899289802601058048L;
    
    private String name;

    private Map<String, RecordIndex> dataFileNameToRecordIndexMap = new HashMap<>();

    public Map<String, RecordIndex> getDataFileNameToRecordIndexMap() {
        return dataFileNameToRecordIndexMap;
    }
    public void setDataFileNameToRecordIndexMap(Map<String, RecordIndex> dataFileNameToRecordIndexMap) {
        this.dataFileNameToRecordIndexMap = dataFileNameToRecordIndexMap;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    private Map<String, Long> fileNameTimeCache = new ConcurrentHashMap<>();
    
    /**
     * @param fromTime
     * @param toTime
     * @return - subset of files in this index that contain data between fromTime and toTime
     */
    @JsonIgnore
    public Set<String> getDataFileNames(long fromTime, long toTime, int minutesPerDataFile) {
        Set<String> ret = new HashSet<>();
        Long ts;
        
        //to catch the first file we need to adjust from time
        fromTime = fromTime - fromTime%TimeUnit.MINUTES.toMillis(minutesPerDataFile);
        
        for(String dfn: dataFileNameToRecordIndexMap.keySet()){
            ts = fileNameTimeCache.get(dfn);
            if(ts == null){
                ts = extractTimeFromTheDataFileName(dfn);
                fileNameTimeCache.put(dfn, ts);
            }
            
            if(ts>=fromTime && ts <=toTime){
                ret.add(dfn);
            }
        }
        return ret;
    }
    
    /**
     * @param dataFileName
     * @return timestamp in ms, extracted from the file name, or -1 if timestamp cannot be extracted
     */
    public static long extractTimeFromTheDataFileName(String dataFileName){
        //data file name is formatted as follows:
        //        Formatter formatter = new Formatter(sb, null);
        //        formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/%s_%d_%d_%4d_%02d_%02d_%02d_%02d_%d.zip",
        //                dsPrefix, customerId, equipmentId, year, month, day, hour,
        //                fileNamePrefix, customerId, equipmentId, year, month, day, hour, minute, createdTs
        //                );
        try{
            String[] parts = dataFileName.split("_");
            int len = parts.length;
            // we are interested in the year, month, day, hour, minute parts
            int year = Integer.parseInt(parts[len-6]);
            int month = Integer.parseInt(parts[len-5]) - 1;
            int day = Integer.parseInt(parts[len-4]);
            int hour = Integer.parseInt(parts[len-3]);
            int minute = Integer.parseInt(parts[len-2]);
            
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.clear();
            c.set(year, month, day, hour, minute, 0);
            
            return c.getTimeInMillis();
        } catch (Exception e){
            return -1;
        }
    }
    
    public static void main(String[] args) {
        long ts = DirectoryIndex.extractTimeFromTheDataFileName("/blah/blah_blah/ree_13_834_2016_12_03_04_26_1480739175816.zip");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX");
        System.out.println("Time: "+ sdf.format(new Date(ts)));
        
    }
}
