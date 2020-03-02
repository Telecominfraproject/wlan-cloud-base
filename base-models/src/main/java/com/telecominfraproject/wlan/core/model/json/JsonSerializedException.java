package com.telecominfraproject.wlan.core.model.json;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.json.JsonSerializedException;

/**
 * @author dtoptygin
 *
 */
public class JsonSerializedException extends BaseJsonModel {
    
    /**
     * 
     */
    private static final long serialVersionUID = 6066070448330619961L;
    private String exType;
    private String error;
    private String path;
    private long timestamp;

    public String getExType() {
        return exType;
    }

    public void setExType(String exType) {
        this.exType = exType;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public JsonSerializedException clone() {
        return (JsonSerializedException) super.clone();
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        return false;
    }
    
}
