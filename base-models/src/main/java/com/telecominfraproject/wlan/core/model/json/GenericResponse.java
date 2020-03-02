package com.telecominfraproject.wlan.core.model.json;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.json.GenericResponse;

/**
 * @author dtoptygin
 *
 */
public class GenericResponse extends BaseJsonModel {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2207675646879789582L;
    private String message;
    private boolean success;

    public GenericResponse() {
        
    }
    
    public GenericResponse(boolean success, String message) {
        this.message = message;
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public GenericResponse clone() {
        return (GenericResponse) super.clone();
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + (success ? 1231 : 1237);
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
        if (!(obj instanceof GenericResponse)) {
            return false;
        }
        GenericResponse other = (GenericResponse) obj;
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
            return false;
        }
        if (success != other.success) {
            return false;
        }
        return true;
    }
    
    
}
