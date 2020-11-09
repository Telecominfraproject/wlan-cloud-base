package com.telecominfraproject.wlan.core.model.webtoken;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class WebTokenRequest extends BaseJsonModel {
    private static final long serialVersionUID = 172984543704305351L;
    
    //{"grantType":"password","userId":"test@test.com","password":"Abcd@1234","scope":"myScope"}
    private String grantType;
    private String userId;
    private String password;
    private String refreshToken;
    private String scope;
    
    public String getGrantType() {
        return grantType;
    }
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getScope() {
        return scope;
    }
    public void setScope(String scope) {
        this.scope = scope;
    }
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
    
}
