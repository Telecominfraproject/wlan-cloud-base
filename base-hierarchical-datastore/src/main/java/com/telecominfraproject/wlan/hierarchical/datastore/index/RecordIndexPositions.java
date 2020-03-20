package com.telecominfraproject.wlan.hierarchical.datastore.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * Class RecordIndexCounts and RecordIndexPositions represent record-level index in hierarchical datastore.
 * The goal of this index is to reduce number of records that need to be processed by json parser when performing filtering operations.
 * This index corresponds on-to-one to a data file in HDS, and it is usually written at the same time as the data file.
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
public class RecordIndexPositions extends BaseJsonModel {

    private static final long serialVersionUID = 17672003429334228L;
    
    private String name;
    private Map<String, List<Integer>> perValuePositions = new HashMap<>();

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Map<String, List<Integer>> getPerValuePositions() {
        return perValuePositions;
    }
    public void setPerValuePositions(Map<String, List<Integer>> perValuePositions) {
        this.perValuePositions = perValuePositions;
    }
    
    @JsonIgnore
    public List<Integer> getPositionsForValue(String value){
        return perValuePositions.getOrDefault(value, Collections.emptyList());
    }
    
    public void addPositionForValue(String value, int pos){
        List<Integer> positions = perValuePositions.get(value);
        if(positions==null){
            positions = new ArrayList<>();
            perValuePositions.put(value, positions);
        }
        
        positions.add(pos);
    }
}
