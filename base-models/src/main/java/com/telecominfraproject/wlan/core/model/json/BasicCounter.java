/**
 * 
 */
package com.telecominfraproject.wlan.core.model.json;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.json.BasicCounter;

/**
 * @author ekeddy
 *
 */
public class BasicCounter extends BaseJsonModel {
    /**
     * 
     */
    private static final long serialVersionUID = -6169900940607546688L;
    private String name;
    private int count;
    
    
    public BasicCounter() {
        super();
    }
    
    public BasicCounter(String name, int count) {
        this.name = name;
        this.count = count;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    
    @Override
    public BasicCounter clone() {
        return (BasicCounter) super.clone();
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }

}
