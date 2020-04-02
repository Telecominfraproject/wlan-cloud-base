package com.telecominfraproject.wlan.core.model.webtoken;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class IntrospectWebTokenResult extends BaseJsonModel {

    private static final long serialVersionUID = 9019848455993644927L;

    private boolean active;
    private int errorCode;
    private String customerId;

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    public int getErrorCode() {
        return errorCode;
    }
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }


}
