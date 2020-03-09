/**
 * 
 */
package com.telecominfraproject.wlan.core.client;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A keep alive strategy uses remote server returned "time-out" value in the header.
 * If the server did not return one, use locally configured value.
 * 
 * We use the {@link DefaultConnectionKeepAliveStrategy} for header parsing.
 * 
 * @author yongli
 *
 */
public class KeepAliveStrategy implements ConnectionKeepAliveStrategy {

    /**
     * default strategy
     */
    private static final ConnectionKeepAliveStrategy DEFAULT_STRATEGY = DefaultConnectionKeepAliveStrategy.INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(KeepAliveStrategy.class);

    /**
     * local idle timeout settings in seconds  
     */
    private long localValue = -1;
    
    /**
     * Construct a KeepAliveStrategy with local override when remote server did not return time-out in the header
     * 
     * @param idleTimeout
     */
    public KeepAliveStrategy(long idleTimeout) {
        this.localValue = idleTimeout;
    }
    
    /* (non-Javadoc)
     * @see org.apache.http.conn.ConnectionKeepAliveStrategy#getKeepAliveDuration(org.apache.http.HttpResponse, org.apache.http.protocol.HttpContext)
     */
    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long value = DEFAULT_STRATEGY.getKeepAliveDuration(response, context);
        if (value < 0) {
            value = localValue;
        }
        LOG.debug("idle timeout set to {} ms", value);
        return value;
    }

}
