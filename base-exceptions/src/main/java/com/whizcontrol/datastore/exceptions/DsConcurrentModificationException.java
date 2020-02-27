package com.whizcontrol.datastore.exceptions;

public class DsConcurrentModificationException extends RuntimeException {

    private static final long serialVersionUID = -4113641519756792128L;

    public DsConcurrentModificationException() {
    }
    
    public DsConcurrentModificationException(Throwable e) {
        super(e);
    }
    
    public DsConcurrentModificationException(String message) {
        super(message);
    }

    public DsConcurrentModificationException(String message, Throwable e) {
        super(message, e);
    }
    
    
}
