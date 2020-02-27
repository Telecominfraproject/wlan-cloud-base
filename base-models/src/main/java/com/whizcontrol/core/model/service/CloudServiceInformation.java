/**
 * 
 */
package com.whizcontrol.core.model.service;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * Cloud Service Information
 * 
 * @author yongli
 *
 */
public class CloudServiceInformation extends BaseJsonModel {
    /**
     * 
     */
    private static final long serialVersionUID = 841621659638248341L;
    /**
     * External hostname
     */
    private String externalHostname;
    /**
     * External port
     */
    private Integer externalPort;

    public CloudServiceInformation() {

    }

    /**
     * Constructor
     * 
     * @param externalHostname
     * @param externalPort
     */
    public CloudServiceInformation(String externalHostname, Integer externalPort) {
        this.externalHostname = externalHostname;
        this.externalPort = externalPort;
    }

    public String getExternalHostname() {
        return externalHostname;
    }

    public void setExternalHostname(String externalHostname) {
        this.externalHostname = externalHostname;
    }

    public Integer getExternalPort() {
        return externalPort;
    }

    public void setExternalPort(Integer externalPort) {
        this.externalPort = externalPort;
    }
    
    public CloudServiceInformation clone() {
        return (CloudServiceInformation) super.clone();
    }
}
