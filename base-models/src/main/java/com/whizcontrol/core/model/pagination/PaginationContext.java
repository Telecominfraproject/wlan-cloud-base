package com.whizcontrol.core.model.pagination;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * This class represents pagination context and keeps track of the last item returned to continue pagination from, number of items per page, as well as various statistics.
 * It can have a list of child pagination contexts - the intent is to use them where multiple tables are being iterated over, with main table and a bunch of dependent tables.
 *    
 * @author dtop
 *
 */
@JsonPropertyOrder(alphabetic=true)
public class PaginationContext<T extends BaseJsonModel> extends BaseJsonModel {
    
    private static final long serialVersionUID = -6139792051056797853L;
    
    private int maxItemsPerPage;
    private int lastReturnedPageNumber;
    private int totalItemsReturned;
    private boolean isLastPage;
    
    private T startAfterItem;
    private Map<String,PaginationContext<? extends BaseJsonModel>> children = new HashMap<>();
   
    public PaginationContext() {
        // for serialization
    }
    
    public PaginationContext(int maxItemsPerPage) {
        this.maxItemsPerPage = maxItemsPerPage;
    }
    
    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }
    public void setMaxItemsPerPage(int maxItemsPerPage) {
        this.maxItemsPerPage = maxItemsPerPage;
    }
    public int getLastReturnedPageNumber() {
        return lastReturnedPageNumber;
    }
    public void setLastReturnedPageNumber(int lastReturnedPageNumber) {
        this.lastReturnedPageNumber = lastReturnedPageNumber;
    }
    public int getTotalItemsReturned() {
        return totalItemsReturned;
    }
    public void setTotalItemsReturned(int totalItemsReturned) {
        this.totalItemsReturned = totalItemsReturned;
    }
    public boolean isLastPage() {
        return isLastPage;
    }
    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }
    public T getStartAfterItem() {
        return startAfterItem;
    }
    public void setStartAfterItem(T startAfterItem) {
        this.startAfterItem = startAfterItem;
    }

    public Map<String, PaginationContext<? extends BaseJsonModel>> getChildren() {
        return children;
    }

    public void setChildren(Map<String, PaginationContext<? extends BaseJsonModel>> children) {
        this.children = children;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaginationContext<T> clone() {
        PaginationContext<T> ret = (PaginationContext<T>) super.clone();
        
        if(startAfterItem!=null){
            ret.startAfterItem = (T) startAfterItem.clone();
        }
        
        if(children!=null){
            ret.children = new HashMap<>();
            for(Map.Entry<String,PaginationContext<? extends BaseJsonModel>> c: children.entrySet()){
                ret.children.put(c.getKey(), c.getValue().clone());
            }
        }
        
        return ret;
    }
    
}
