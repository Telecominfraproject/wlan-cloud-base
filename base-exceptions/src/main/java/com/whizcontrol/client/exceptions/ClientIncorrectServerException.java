/**
 * 
 */
package com.whizcontrol.client.exceptions;

/**
 * Exception with message set to the remote URL the request should be redirect
 * to.
 * 
 * @author yongli
 *
 */
public class ClientIncorrectServerException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 5241814854199409694L;

    /**
     * Construct the exception with the server URL to redirect the request to.
     * 
     * @param remoteServerURL
     */
    public ClientIncorrectServerException(String remoteServerURL) {
        super(remoteServerURL);
    }

    public String getServerBaseURL() {
        // TODO enhance this
        return getLocalizedMessage();
    }

}
