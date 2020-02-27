package com.whizcontrol.datastore.exceptions;

public class DsForeignKeyViolatedException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -8763622799787883013L;

    public DsForeignKeyViolatedException() {
    }
    
    public DsForeignKeyViolatedException(Throwable e) {
        super(e);
    }
    
    public DsForeignKeyViolatedException(String message) {
        super(message);
    }

    public DsForeignKeyViolatedException(String message, Throwable e) {
        super(message, e);
    }
    
    
}
