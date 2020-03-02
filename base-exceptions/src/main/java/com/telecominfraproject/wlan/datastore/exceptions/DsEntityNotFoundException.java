package com.telecominfraproject.wlan.datastore.exceptions;

public class DsEntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -4113641519756792128L;

    public DsEntityNotFoundException() {
    }
    
    public DsEntityNotFoundException(Throwable e) {
        super(e);
    }
    
    public DsEntityNotFoundException(String message) {
        super(message);
    }

    public DsEntityNotFoundException(String message, Throwable e) {
        super(message, e);
    }
    
    
}
