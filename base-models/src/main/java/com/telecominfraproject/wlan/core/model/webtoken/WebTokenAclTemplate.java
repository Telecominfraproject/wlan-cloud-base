package com.telecominfraproject.wlan.core.model.webtoken;

import java.util.HashMap;
import java.util.Map;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class WebTokenAclTemplate extends BaseJsonModel {
    
    private static final long serialVersionUID = 9019848455993644927L;
    
    private Map<String, Boolean> aclTemplate = new HashMap<>();    
    private int alias = 2;
    private String id = "";
    private String name = "Webtoken Portal Login";
    private long versionTimestamp = 1551549825443000L;
    
    public WebTokenAclTemplate() {
        aclTemplate.put("Delete", true);
        aclTemplate.put("Read", true);
        aclTemplate.put("ReadWrite", true);
        aclTemplate.put("ReadWriteCreate", true);
        aclTemplate.put("PortalLogin", true);
    }
    
    public int getAlias() {
        return alias;
    }
    public void setAlias(int alias) {
        this.alias = alias;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getVersionTimestamp() {
        return versionTimestamp;
    }
    public void setVersionTimestamp(long versionTimestamp) {
        this.versionTimestamp = versionTimestamp;
    }
    public Map<String, Boolean> getAclTemplate() {
        return aclTemplate;
    }
    public void setAclTemplate(Map<String, Boolean> aclTemplate) {
        this.aclTemplate = aclTemplate;
    }
    
    
}
