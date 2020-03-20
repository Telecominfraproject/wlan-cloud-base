package com.telecominfraproject.wlan.hierarchical.datastore.index;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * Class RecordIndexCounts and RecordIndexPositions represent record-level index in hierarchical datastore.
 * The goal of this index is to reduce number of records that need to be processed by json parser when performing filtering operations.
 * This index corresponds one-to-one to a data file, and it is usually written at the same time as the data file.
 * During data filtering operations the index should be taken as a hint - if it is missing, then full data file will be processed.
 * It should be possible to introduce new indexes after the fact - old data files can be scanned and new index files can be created.
 * Indexes are stored in the same directory as the data files they represent.
 * <br>
 * Index file name is structured as idx_[indexName]_[dataFileName] and it is not compressed.
 * Inside the index file archive there is one entry with a text file.
 * First line in that text file contains json object for RecordIndexCounts, second line contains json object for RecordIndexPositions  
 *   
 * @author dtop
 *
 */
public class RecordIndexCounts extends BaseJsonModel {

    private static final long serialVersionUID = 17672003429334228L;
    
    private String name;
    private int totalCount;
    private Map<String, Integer> perValueCounts = new HashMap<>();

    public int getTotalCount() {
        return totalCount;
    }
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public Map<String, Integer> getPerValueCounts() {
        return perValueCounts;
    }
    public void setPerValueCounts(Map<String, Integer> perValueCounts) {
        this.perValueCounts = perValueCounts;
    }

    @JsonIgnore
    public int getCountForValue(String value){
        return perValueCounts.getOrDefault(value, 0);
    }
    
    public void incrementCountForValue(String value){
        totalCount++;
        perValueCounts.put(value, getCountForValue(value) + 1 );
    }
}
