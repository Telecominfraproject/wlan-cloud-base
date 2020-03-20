package com.telecominfraproject.wlan.core.server.jdbc;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * @author dtop
 *
 */
public abstract class BaseJdbcDao {
    /**
     * Wait to up to 5 seconds to catch up with current last mod
     */
    private static final long NEXT_LASTMOD_WAIT_THRESHOLD = 5;
    
    private static final Logger LOG = LoggerFactory.getLogger(BaseJdbcDao.class);
    
    protected JdbcOperations jdbcTemplate;
    
    /**
     * Key converter used for converting generated key column name
     */
    protected BaseKeyColumnConverter keyColumnConverter;

    private boolean skipCheckForConcurrentUpdates;

    
    @Autowired(required=false)
    public void setDataSource(DataSource dataSource) {
        if(this.jdbcTemplate==null){
            LOG.debug("{} uses datasource {}", this.getClass().getSimpleName(), dataSource);
            JdbcTemplate jt = new JdbcTemplate(dataSource, false);
            
            //wrap and register jdbcTemplate object to produce JMX metrics 
//            JdbcOperations ret = TimedInterface.newProxy(JdbcOperations.class, jt, "JdbcTemplate-"+this.getClass().getSimpleName());
//            DefaultMonitorRegistry.getInstance().register((CompositeMonitor)ret);

            //build user-friendly metrics Id - remove $$EnhancedByCGlib... at the end of the class name
            String metricsId = this.getClass().getSimpleName();
            int idx = metricsId.indexOf('$');
            if(idx>0){
                metricsId = metricsId.substring(0, idx);
            }
            
            JdbcOperations ret = new JdbcOperationsWithMetrics(jt, metricsId);
            
            this.jdbcTemplate = ret;
        }
        
        if (this.keyColumnConverter == null) {
            if (dataSource instanceof BaseJDbcDataSource) {
                this.keyColumnConverter = ( (BaseJDbcDataSource) dataSource).getKeyColumnConverter();
            }

            if (null == this.keyColumnConverter) {
                // use default one
                this.keyColumnConverter = new KeyColumnConverter();
            }
        }
    }

    
    @Value("${skipCheckForConcurrentUpdates:false}")
    private void setSkipCheckForConcurrentUpdates(String skipCheckForConcurrentUpdatesStr) {
        this.skipCheckForConcurrentUpdates = Boolean.parseBoolean(skipCheckForConcurrentUpdatesStr);
    }
    
    /**
     * Use this method only for testing.
     * Normally the value for this property is set via application.properties or via -DskipCheckForConcurrentUpdates=true
     * Default value is false, which means to USE checks for concurrent updates.
     * @param skipCheckForConcurrentUpdates
     */
    public void setSkipCheckForConcurrentUpdates(boolean skipCheckForConcurrentUpdates) {
        this.skipCheckForConcurrentUpdates = skipCheckForConcurrentUpdates;
    }

    public boolean isSkipCheckForConcurrentUpdates() {
        return skipCheckForConcurrentUpdates;
    }
    
    /**
     * Create the last modified timestamp based on the current one
     * 
     * @param currentLastModTs
     * @return new last modified TS
     */
    protected static long getNewLastModTs(long currentLastModTs) {
        long result = System.currentTimeMillis();
        while (result <= currentLastModTs) {
            long diff = currentLastModTs - result;
            if (diff > TimeUnit.SECONDS.toMillis(NEXT_LASTMOD_WAIT_THRESHOLD)) {
                throw new GenericErrorException("Existing last modified TS is in the future");
            }
            if (diff > 0) {
                // pause till we have a time great than current lastMod
                try {
                    Thread.sleep(diff + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GenericErrorException("Unable to generate the new last modified TS", e);
                }
            }
            result = System.currentTimeMillis();
        }
        return result;
    }
}
