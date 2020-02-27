/**
 * 
 */
package com.whizcontrol.server.exceptions;

/**
 * Generic wrapper exception
 * 
 * @author yongli
 *
 */
public class GenericErrorException extends RuntimeException {
    private static final long serialVersionUID = 5761427845544531472L;

    public GenericErrorException() {
    }
    
    public GenericErrorException(Throwable e) {
        super(e);
    }
    
    public GenericErrorException(String message) {
        super(message);
    }

    public GenericErrorException(String message, Throwable e) {
        super(message, e);
    }
}
