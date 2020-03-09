package com.telecominfraproject.wlan.core.client.models;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtoptygin
 *
 */
public class HttpClientCredentials extends BaseJsonModel {
    private static final long serialVersionUID = -4090785689738119033L;
    private String host;
    private int port;
    private String user;
    private String password;
    
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public HttpClientCredentials clone() {
        return (HttpClientCredentials) super.clone();
    }
}
