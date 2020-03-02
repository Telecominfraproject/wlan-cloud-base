/**
 * 
 */
package com.telecominfraproject.wlan.core.model.algorithms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

/**
 * @author erik
 *
 */
public enum CellSizeAlgorithm {
    neighbours_based(1),
    noise_floor_based(2),

    UNSUPPORTED(-1);

    @JsonCreator
    public static CellSizeAlgorithm getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, CellSizeAlgorithm.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(CellSizeAlgorithm value) {
        return (UNSUPPORTED.equals(value));
    }

    private final int id;

    CellSizeAlgorithm(int id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
