package com.whizcontrol.core.model.pagination;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;

/**
 * @author dtop
 *
 */
public enum SortOrder {
    asc,
    desc,
    UNSUPPORTED;
    
    public static boolean isUnsupported(SortOrder value) {
        return SortOrder.UNSUPPORTED.equals(value);
    }

    @JsonCreator
    public static SortOrder getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, SortOrder.class, UNSUPPORTED);
    }

}
