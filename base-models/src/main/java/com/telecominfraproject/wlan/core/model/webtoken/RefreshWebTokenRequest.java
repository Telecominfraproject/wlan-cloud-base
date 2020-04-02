package com.telecominfraproject.wlan.core.model.webtoken;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class RefreshWebTokenRequest extends BaseJsonModel {
    private static final long serialVersionUID = 172984543704305352L;
    
    //{"grantType":"password","refreshToken":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ4YyIsImp0aSI6IjM5YmZmNmM3LWY4MWYtNDg1NS1iZGRlLTExZDBmNGIzYWM5YiJ9.Jk2YDXMz1_93DulIIlA3MzekRVNH0arUlbwbyIIJkbg","scope":"myScope"}
    private String grantType;
    private String refreshToken;
    private String scope;
    
    public String getGrantType() {
        return grantType;
    }
    public void setGrantType(String grantType) {
        this.grantType = grantType;
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
