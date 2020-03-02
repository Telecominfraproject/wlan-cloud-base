package com.telecominfraproject.wlan.datastore.exceptions;

public class DsInvalidNumberEntitiesReturned extends RuntimeException {
    private static final long serialVersionUID = -3714845675690578087L;

    public DsInvalidNumberEntitiesReturned() {

    }

    public DsInvalidNumberEntitiesReturned(String msg) {
        super(msg);
    }

    public DsInvalidNumberEntitiesReturned(int numberExpected, int numberReturned) {
        this("numberExpected:" + numberExpected + ", numberReturned:" + numberReturned);
    }

}
