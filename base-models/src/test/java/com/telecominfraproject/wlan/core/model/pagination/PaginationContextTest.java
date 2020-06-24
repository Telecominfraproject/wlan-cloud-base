package com.telecominfraproject.wlan.core.model.pagination;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.pagination.PaginationContext;
import com.telecominfraproject.wlan.core.model.pair.PairIntString;
import com.telecominfraproject.wlan.core.model.pair.PairLongLong;
import com.telecominfraproject.wlan.core.model.pair.PairStringLong;

public class PaginationContextTest {

    @Test
    public void testSerialization() {
        
        PaginationContext<PairLongLong> pll = new PaginationContext<>(10);
        pll.setStartAfterItem(new PairLongLong());
        
        PaginationContext<PairStringLong> psl = new PaginationContext<>(10);
        psl.setStartAfterItem(new PairStringLong("str1", 42L));
        pll.getChildren().getChildren().put("psl", psl);
        
        assertEquals("{\"model_type\":\"PaginationContext\","
        		+ "\"cursor\":\"eyJtb2RlbF90eXBlIjoiUGFpclN0cmluZ0xvbmciLCJ2YWx1ZTEiOiJzdHIxIiwidmFsdWUyIjo0Mn1AQEB7Im1vZGVsX3R5cGUiOiJDb250ZXh0Q2hpbGRyZW4iLCJjaGlsZHJlbiI6e319QEBAbnVsbA==\","
        		+ "\"lastPage\":false,\"lastReturnedPageNumber\":0,\"maxItemsPerPage\":10,\"totalItemsReturned\":0}", psl.toString());
        
        assertEquals("{\"model_type\":\"PaginationContext\",\"cursor\":\"eyJtb2RlbF90eXBlIjoiUGFpckxvbmdMb25nIiwidmFsdWUxIjpudWxsLCJ2YWx1ZTIiOm51bGx9QEBAeyJtb2RlbF90eXBlIjoiQ29udGV4dENoaWxkcmVuIiwiY2hpbGRyZW4iOnsicHNsIjp7Im1vZGVsX3R5cGUiOiJQYWdpbmF0aW9uQ29udGV4dCIsImN1cnNvciI6ImV5SnRiMlJsYkY5MGVYQmxJam9pVUdGcGNsTjBjbWx1WjB4dmJtY2lMQ0oyWVd4MVpURWlPaUp6ZEhJeElpd2lkbUZzZFdVeUlqbzBNbjFBUUVCN0ltMXZaR1ZzWDNSNWNHVWlPaUpEYjI1MFpYaDBRMmhwYkdSeVpXNGlMQ0pqYUdsc1pISmxiaUk2ZTMxOVFFQkFiblZzYkE9PSIsImxhc3RQYWdlIjpmYWxzZSwibGFzdFJldHVybmVkUGFnZU51bWJlciI6MCwibWF4SXRlbXNQZXJQYWdlIjoxMCwidG90YWxJdGVtc1JldHVybmVkIjowfX19QEBAbnVsbA==\","
        		+ "\"lastPage\":false,\"lastReturnedPageNumber\":0,\"maxItemsPerPage\":10,\"totalItemsReturned\":0}", pll.toString());
        
    }
    
    @Test
    public void testCursorSerialization() {
        PaginationContext<PairIntString> context = new PaginationContext<>(10);
        assertEquals(context.toString(), BaseJsonModel.fromString(context.toString(), BaseJsonModel.class).toString());
//        System.out.println("-1-" + context);
//        System.out.println("-2-" + BaseJsonModel.fromString(context.toString(), BaseJsonModel.class));
        context.setStartAfterItem(new PairIntString(42,"myValue"));
        assertEquals(context.toString(), BaseJsonModel.fromString(context.toString(), BaseJsonModel.class).toString());
        
        PaginationContext<PairIntString> childContext = new PaginationContext<>(10);
        childContext.setStartAfterItem(new PairIntString(43,"myValueChild"));
        
        context.getChildren().getChildren().put("child", childContext);
        assertEquals(context.toString(), BaseJsonModel.fromString(context.toString(), BaseJsonModel.class).toString());

        
        PaginationContext<PairIntString> contextWithThirdParty = new PaginationContext<>(10);
        assertEquals(contextWithThirdParty.toString(), BaseJsonModel.fromString(contextWithThirdParty.toString(), BaseJsonModel.class).toString());
        contextWithThirdParty.setStartAfterItem(new PairIntString(42,"myValue"));
        contextWithThirdParty.setThirdPartyPagingState(new byte[] {0,1,42});
        assertEquals(contextWithThirdParty.toString(), BaseJsonModel.fromString(contextWithThirdParty.toString(), BaseJsonModel.class).toString());

    }

}
