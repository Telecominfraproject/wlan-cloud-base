package com.telecominfraproject.wlan.core.model.pagination;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.pagination.PaginationContext;
import com.telecominfraproject.wlan.core.model.pair.PairLongLong;
import com.telecominfraproject.wlan.core.model.pair.PairStringLong;

public class PaginationContextTest {

    @Test
    public void testSerialization() {
        
        PaginationContext<PairLongLong> pll = new PaginationContext<>(10);
        pll.setStartAfterItem(new PairLongLong());
        
        PaginationContext<PairStringLong> psl = new PaginationContext<>(10);
        psl.setStartAfterItem(new PairStringLong("str1", 42L));
        pll.getChildren().put("psl", psl);
        
        assertEquals("{\"_type\":\"PaginationContext\","
        		+ "\"children\":{},"
        		+ "\"lastPage\":false,"
        		+ "\"lastReturnedPageNumber\":0,"
        		+ "\"maxItemsPerPage\":10,"
        		+ "\"startAfterItem\":{\"_type\":\"PairStringLong\",\"value1\":\"str1\",\"value2\":42},"
        		+ "\"totalItemsReturned\":0}", psl.toString());
        
        assertEquals("{\"_type\":\"PaginationContext\","
        		+ "\"children\":{"
        		+ 	"\"psl\":{\"_type\":\"PaginationContext\","
        		+ 	"\"children\":{},\"lastPage\":false,"
        		+ 	"\"lastReturnedPageNumber\":0,"
        		+ 	"\"maxItemsPerPage\":10,"
        		+ 	"\"startAfterItem\":{\"_type\":\"PairStringLong\",\"value1\":\"str1\",\"value2\":42},"
        		+ 	"\"totalItemsReturned\":0}"
        		+ 	"},"
        		+ "\"lastPage\":false,"
        		+ "\"lastReturnedPageNumber\":0,"
        		+ "\"maxItemsPerPage\":10,"
        		+ "\"startAfterItem\":{\"_type\":\"PairLongLong\",\"value1\":null,\"value2\":null},"
        		+ "\"totalItemsReturned\":0}", pll.toString());
        
    }

}
