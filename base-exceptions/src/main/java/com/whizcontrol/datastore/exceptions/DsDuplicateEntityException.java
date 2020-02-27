package com.whizcontrol.datastore.exceptions;

public class DsDuplicateEntityException extends RuntimeException {

    private static final long serialVersionUID = -4113641519756792128L;

    public DsDuplicateEntityException() {
    }
    
    public DsDuplicateEntityException(Throwable e) {
        super(e);
    }
    
    public DsDuplicateEntityException(String message) {
        super(message);
    }

    public DsDuplicateEntityException(String message, Throwable e) {
        super(message, e);
    }
    
    
}
