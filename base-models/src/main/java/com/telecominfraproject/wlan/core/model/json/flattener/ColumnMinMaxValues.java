package com.telecominfraproject.wlan.core.model.json.flattener;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 * Class that keeps track of min/max values for each column index.
 */
public class ColumnMinMaxValues extends BaseJsonModel {

    private static final long serialVersionUID = 4965204045619150121L;

    private double[] minValues;
    private double[] maxValues;
    
    public double[] getMinValues() {
        return minValues;
    }
    public void setMinValues(double[] minValues) {
        this.minValues = minValues;
    }
    public double[] getMaxValues() {
        return maxValues;
    }
    public void setMaxValues(double[] maxValues) {
        this.maxValues = maxValues;
    }
    
    
}
