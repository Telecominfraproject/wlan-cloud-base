package com.whizcontrol.core.server.controller.ping;

import com.whizcontrol.core.model.json.BaseJsonModel;

public class PingResponse extends BaseJsonModel {

    /**
     * 
     */
    private static final long serialVersionUID = 5627721113547873084L;
    
    private long startupTime;
    private long currentTime;
    private String applicationName;
    private String hostName;
    
    protected PingResponse()
    {
        super();
    }
    
    public PingResponse(long startupTime, long currentTime, String applicationName, String hostName){
        this.startupTime = startupTime;
        this.currentTime = currentTime;
        this.applicationName = applicationName;
        this.hostName = hostName;
    }
    
    public long getStartupTime() {
        return startupTime;
    }
    public long getCurrentTime() {
        return currentTime;
    }
    public String getApplicationName() {
        return applicationName;
    }

    public String getHostName() {
        return hostName;
    }
    
}
