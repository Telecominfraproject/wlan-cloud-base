/**
 * 
 */
package com.whizcontrol.models.exceptions;

/**
 * Unable to instantiate object from class.
 * 
 * @author yongli
 *
 */
public class DataInstantiationException extends RuntimeException {
    private static final long serialVersionUID = 969601174773643535L;

    public DataInstantiationException() {
    }

    public DataInstantiationException(Class<?> clazz, Throwable e) {
        super("Unable to instantiate " + clazz.getSimpleName(), e);
    }

    public DataInstantiationException(Throwable e) {
        super(e);
    }

    public DataInstantiationException(String message, Throwable e) {
        super(message, e);
    }

    public DataInstantiationException(String message) {
        super(message);
    }
}
