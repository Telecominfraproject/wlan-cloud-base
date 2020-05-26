package com.telecominfraproject.wlan.core.model.pagination;

import java.util.HashMap;
import java.util.Map;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class ContextChildren extends BaseJsonModel {

	private static final long serialVersionUID = 3084944705321881458L;
	
	private Map<String,PaginationContext<? extends BaseJsonModel>> children = new HashMap<>();
    
    public Map<String, PaginationContext<? extends BaseJsonModel>> getChildren() {
		return children;
	}

	public void setChildren(Map<String, PaginationContext<? extends BaseJsonModel>> children) {
		this.children = children;
	}

	@Override
    public ContextChildren clone() {
        ContextChildren ret = (ContextChildren) super.clone();

    	if(children!=null){
            ret.children = new HashMap<>();
            for(Map.Entry<String,PaginationContext<? extends BaseJsonModel>> c: children.entrySet()){
                ret.children.put(c.getKey(), c.getValue().clone());
            }
        }
        
        return ret;
    }
}
