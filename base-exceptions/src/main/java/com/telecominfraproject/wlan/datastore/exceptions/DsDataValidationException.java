package com.telecominfraproject.wlan.datastore.exceptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Invalid configuration data
 * 
 * @author yongli
 *
 */
public class DsDataValidationException extends RuntimeException {

    private static final long serialVersionUID = -2711151397859497210L;
    
    private List<String> invalidFields = new ArrayList<>();
    
    public DsDataValidationException() {
    }
    
    public DsDataValidationException(Throwable e) {
        super(e);
    }
    
    public DsDataValidationException(String message) {
        super(message);
    }

    public DsDataValidationException(String message, Throwable e) {
        super(message, e);
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }

    public void setInvalidFields(List<String> invalidFields) {
        this.invalidFields = invalidFields;
    }
}
