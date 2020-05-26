package com.telecominfraproject.wlan.core.model.pagination;

import java.nio.charset.Charset;

import org.springframework.util.Base64Utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.pair.PairIntString;

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

	private static final String CURSOR_SEPARATOR = "@@@";
	private static final Charset utf8 = Charset.forName("UTF-8");

    
    private int maxItemsPerPage = 20;
    private int lastReturnedPageNumber;
    private int totalItemsReturned;
    private boolean isLastPage;
    
    private T startAfterItem;
    private ContextChildren children = new ContextChildren();
       
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
    
    /**
     * @see getCursor/setCursor
     */
    @JsonIgnore
    public T getStartAfterItem() {
        return startAfterItem;
    }

    /**
     * @see getCursor/setCursor
     */
    @JsonIgnore
    public void setStartAfterItem(T startAfterItem) {
        this.startAfterItem = startAfterItem;
    }

    /**
     * @see getCursor/setCursor
     */    
    @JsonIgnore
    public ContextChildren getChildren() {
        return children;
    }

    /**
     * @see getCursor/setCursor
     */
    @JsonIgnore
    public void setChildren(ContextChildren children) {
        this.children = children;
    }

    /**
     * @return opaque string, which contains a base64-encoded values of properties children and startAfterItem
     */
    public String getCursor() {
    	StringBuilder strb = new StringBuilder(512);
    	
    	strb.append(startAfterItem!=null?startAfterItem:"null");
    	strb.append(CURSOR_SEPARATOR);
    	strb.append(children!=null?children:"null");
    	
		return Base64Utils.encodeToString(strb.toString().getBytes(utf8 ));
    }
    
    /**
     * Set values of properties children and startAfterItem from the opaque base64-encoded string 
     */
    public void setCursor(String cursor) {
    	if(cursor == null || cursor.isEmpty()) {
    		return;
    	}
    	
    	String decodedCursor = new String(Base64Utils.decodeFromString(cursor), utf8);
    	int separatorStartPos = decodedCursor.indexOf(CURSOR_SEPARATOR);
    	if(separatorStartPos<0) {
    		throw new IllegalArgumentException("Cannot parse cursor: separator not found");
    	}
    	String startAfterItemStr = decodedCursor.substring(0, separatorStartPos);

    	if(separatorStartPos +  CURSOR_SEPARATOR.length() >= decodedCursor.length()) {
    		throw new IllegalArgumentException("Cannot parse cursor");
    	}

    	String childrenStr = decodedCursor.substring(separatorStartPos + CURSOR_SEPARATOR.length());
    	
    	if(!startAfterItemStr.equals("null")) {
    		startAfterItem = (T) BaseJsonModel.fromString(startAfterItemStr, BaseJsonModel.class);
    	}
    	
    	if(!childrenStr.equals("null")) {
    		children = (ContextChildren) BaseJsonModel.fromString(childrenStr, BaseJsonModel.class);
    	}

    }
    
    @Override
    @SuppressWarnings("unchecked")
    public PaginationContext<T> clone() {
        PaginationContext<T> ret = (PaginationContext<T>) super.clone();
        
        if(startAfterItem!=null){
            ret.startAfterItem = (T) startAfterItem.clone();
        }
        
        if(children!=null){
            ret.children = children.clone();
        }
        
        return ret;
    }
    
}
