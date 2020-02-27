package com.whizcontrol.core.model.pagination;

import java.util.ArrayList;
import java.util.List;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class PaginationResponse<T extends BaseJsonModel> extends BaseJsonModel {
    
    private static final long serialVersionUID = 7461927798155325485L;
    private List<T> items = new ArrayList<>();
    private PaginationContext<T> context;
    
    public PaginationResponse() {
        // for serialization
    }
    
    public PaginationResponse(PaginationContext<T> context) {
        this.context = context;
    }    
    
    public PaginationContext<T> getContext() {
        return context;
    }
    public void setContext(PaginationContext<T> context) {
        this.context = context;
    }
    public List<T> getItems() {
        return items;
    }
    public void setItems(List<T> items) {
        this.items = items;
    }

    /**
     * Adjust pagination context according to the last retrieved page of data
     */
    public void prepareForNextPage() {
        if(context == null){
            return;
        }
        
        context.setLastPage(items.size() < context.getMaxItemsPerPage());
        context.setLastReturnedPageNumber(context.getLastReturnedPageNumber() + 1);
        if(!items.isEmpty() && !context.isLastPage()){
            context.setStartAfterItem(items.get(items.size() - 1));
        } else {
            context.setStartAfterItem(null);
        }
        context.setTotalItemsReturned(context.getTotalItemsReturned() + items.size());
    }

}
