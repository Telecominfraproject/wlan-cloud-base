package com.telecominfraproject.wlan.hierarchical.datastore.index;

import java.util.Set;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public interface RecordIndexValueExtractor {
    /**
     * 
     * @param model
     * @return Set of index values that are extracted from the supplied model. If no values are extracted, then returned set will contain one empty string "".
     */
    Set<String> extractValues(BaseJsonModel model);
}
