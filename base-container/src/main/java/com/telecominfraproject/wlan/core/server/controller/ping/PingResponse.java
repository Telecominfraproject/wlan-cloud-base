package com.telecominfraproject.wlan.core.server.controller.ping;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class PingResponse extends BaseJsonModel {

    /**
     * 
     */
    private static final long serialVersionUID = 5627721113547873084L;
    
    private long startupTime;
    private long currentTime;
    private String applicationName;
    private String hostName;

	private String commitID;
	private String commitDate;
	private String projectVersion;
	
	
	
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
    
    public PingResponse(long startupTime, long currentTime, String applicationName, String hostName, String commitID, String commitDate, String projectVersion){
        this(startupTime, currentTime, applicationName, hostName);
        this.commitID = commitID;
        this.commitDate = commitDate;
        this.projectVersion = projectVersion;        
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
        
    public String getCommitID() {
        return commitID;        
    }
    
    public String getCommitDate() {
        return commitDate;        
    }
    
    public String getProjectVersion() {
        return projectVersion;        
    }
    
    
}
