package com.telecominfraproject.wlan.hierarchical.datastore.index;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class RecordIndex extends BaseJsonModel {
    private static final long serialVersionUID = 4535638079969100441L;

    private RecordIndexCounts counts;
    private RecordIndexPositions positions;
    
    public RecordIndex(){}
    
    public RecordIndex(RecordIndexCounts counts, RecordIndexPositions positions){
        this.counts = counts;
        this.positions = positions;
    }

    public RecordIndexCounts getCounts() {
        return counts;
    }
    public void setCounts(RecordIndexCounts counts) {
        this.counts = counts;
    }
    public RecordIndexPositions getPositions() {
        return positions;
    }
    public void setPositions(RecordIndexPositions positions) {
        this.positions = positions;
    }
    
    
}
