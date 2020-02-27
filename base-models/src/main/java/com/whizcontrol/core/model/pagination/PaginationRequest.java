package com.whizcontrol.core.model.pagination;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class PaginationRequest<T extends BaseJsonModel> extends BaseJsonModel {

    private static final long serialVersionUID = -8457714747363287890L;
    private PaginationContext<T> context;
    
    public PaginationContext<T> getContext() {
        return context;
    }
    public void setContext(PaginationContext<T> context) {
        this.context = context;
    }
    
}
