package com.telecominfraproject.wlan.core.model.utils;

public class JsonPatchException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 2416996052311290335L;
    private Throwable exception;

    public JsonPatchException() {

    }

    public JsonPatchException(Throwable except) {
        this.exception = except;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

}
