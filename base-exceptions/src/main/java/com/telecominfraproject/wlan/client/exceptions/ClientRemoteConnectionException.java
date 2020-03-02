/**
 * 
 */
package com.telecominfraproject.wlan.client.exceptions;

/**
 * Remote API Call connection exception
 * 
 * @author yongli
 *
 */
public class ClientRemoteConnectionException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -1215543900988425727L;

    public ClientRemoteConnectionException() {
    }

    public ClientRemoteConnectionException(Throwable e) {
        super(e);
    }

    public ClientRemoteConnectionException(String message) {
        super(message);
    }

    public ClientRemoteConnectionException(String message, Throwable e) {
        super(message, e);
    }
}
