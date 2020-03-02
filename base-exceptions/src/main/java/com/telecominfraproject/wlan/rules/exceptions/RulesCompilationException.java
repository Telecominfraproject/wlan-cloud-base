package com.telecominfraproject.wlan.rules.exceptions;

/**
 * @author dtop
 *
 */
public class RulesCompilationException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -305890162567869883L;

    public RulesCompilationException() {
    }
    
    public RulesCompilationException(Throwable e) {
        super(e);
    }
    
    public RulesCompilationException(String message, Throwable e) {
        super(message, e);
    }

    public RulesCompilationException(String message) {
        super(message);
    }

}
