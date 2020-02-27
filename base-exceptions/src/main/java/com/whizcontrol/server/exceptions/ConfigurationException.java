package com.whizcontrol.server.exceptions;

/**
 * @author dtop
 *
 */
public class ConfigurationException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -305890162567869883L;

    public ConfigurationException() {
    }
    
    public ConfigurationException(Throwable e) {
        super(e);
    }
    
    public ConfigurationException(String message, Throwable e) {
        super(message, e);
    }

    public ConfigurationException(String message) {
        super(message);
    }

}
