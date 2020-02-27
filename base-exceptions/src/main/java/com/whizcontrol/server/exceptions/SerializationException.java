package com.whizcontrol.server.exceptions;

/**
 * @author dtop
 *
 */
public class SerializationException extends RuntimeException {


    private static final long serialVersionUID = 5358302874364339037L;

    public SerializationException() {
    }
    
    public SerializationException(Throwable e) {
        super(e);
    }
    
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable e) {
        super(message, e);
    }

}
