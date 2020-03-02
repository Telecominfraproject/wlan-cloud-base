package com.telecominfraproject.wlan.core.model.json.flattener;

import java.util.Map;

/**
 * @author dtop
 * Container for individual row of values with a timestamp, sortable by timestamp. 
 */
public class ValueElementMap implements Comparable<ValueElementMap>{
    long timestampMs;
    Map<String, Object> values;
    
    public ValueElementMap(long timestampMs, Map<String, Object> values) {
        this.timestampMs = timestampMs;
        this.values = values;
    }
    
    @Override
    public int compareTo(ValueElementMap other) {
        return Long.compare(this.timestampMs, other.timestampMs);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (timestampMs ^ (timestampMs >>> 32));
        result = prime * result + ((values == null) ? 0 : values.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ValueElementMap)) {
            return false;
        }
        ValueElementMap other = (ValueElementMap) obj;
        if (timestampMs != other.timestampMs) {
            return false;
        }
        if (values == null) {
            if (other.values != null) {
                return false;
            }
        } else if (!values.equals(other.values)) {
            return false;
        }
        return true;
    }
    
    
    
}