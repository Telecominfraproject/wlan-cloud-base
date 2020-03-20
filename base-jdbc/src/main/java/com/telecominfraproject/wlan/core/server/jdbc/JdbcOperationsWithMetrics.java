package com.telecominfraproject.wlan.core.server.jdbc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;

/**
 * @author dtop
 *
 */
class JdbcOperationsWithMetrics implements JdbcOperations{
    
    private final TagList tags = CloudMetricsTags.commonTags;
    
    private final Counter queriesExecuted;
    private final Counter updatesExecuted;
    private final Counter execsExecuted;
    private final Counter callsExecuted;
    private final Counter batchesExecuted;

    private final Counter queriesErrors;
    private final Counter updatesErrors;
    private final Counter execsErrors;
    private final Counter callsErrors;
    private final Counter batchesErrors;

    private final Timer queriesTimer;
    private final Timer updatesTimer;
    private final Timer execsTimer;
    private final Timer callsTimer;
    private final Timer batchesTimer;
    
    private final JdbcOperations delegate;
    
    public JdbcOperationsWithMetrics(JdbcOperations delegate, String metricsId) {
        this.delegate = delegate;
        
        queriesExecuted = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-queriesExecuted").withTags(tags).build());
        updatesExecuted = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-updatesExecuted").withTags(tags).build());
        execsExecuted = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-execsExecuted").withTags(tags).build());
        callsExecuted = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-callsExecuted").withTags(tags).build());
        batchesExecuted = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-batchesExecuted").withTags(tags).build());

        queriesErrors = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-queriesErrors").withTags(tags).build());
        updatesErrors = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-updatesErrors").withTags(tags).build());
        execsErrors = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-execsErrors").withTags(tags).build());
        callsErrors = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-callsErrors").withTags(tags).build());
        batchesErrors = new BasicCounter(MonitorConfig.builder("Jdbc-"+metricsId + "-batchesErrors").withTags(tags).build());

        queriesTimer = new BasicTimer(MonitorConfig.builder("Jdbc-"+metricsId + "-queriesTimer").withTags(tags).build());
        updatesTimer = new BasicTimer(MonitorConfig.builder("Jdbc-"+metricsId + "-updatesTimer").withTags(tags).build());
        execsTimer = new BasicTimer(MonitorConfig.builder("Jdbc-"+metricsId + "-execsTimer").withTags(tags).build());
        callsTimer = new BasicTimer(MonitorConfig.builder("Jdbc-"+metricsId + "-callsTimer").withTags(tags).build());
        batchesTimer = new BasicTimer(MonitorConfig.builder("Jdbc-"+metricsId + "-batchesTimer").withTags(tags).build());
        
        DefaultMonitorRegistry.getInstance().register(queriesExecuted);
        DefaultMonitorRegistry.getInstance().register(updatesExecuted);
        DefaultMonitorRegistry.getInstance().register(execsExecuted);
        DefaultMonitorRegistry.getInstance().register(callsExecuted);
        DefaultMonitorRegistry.getInstance().register(batchesExecuted);

        DefaultMonitorRegistry.getInstance().register(queriesErrors);
        DefaultMonitorRegistry.getInstance().register(updatesErrors);
        DefaultMonitorRegistry.getInstance().register(execsErrors);
        DefaultMonitorRegistry.getInstance().register(callsErrors);
        DefaultMonitorRegistry.getInstance().register(batchesErrors);

        DefaultMonitorRegistry.getInstance().register(queriesTimer);
        DefaultMonitorRegistry.getInstance().register(updatesTimer);
        DefaultMonitorRegistry.getInstance().register(execsTimer);
        DefaultMonitorRegistry.getInstance().register(callsTimer);
        DefaultMonitorRegistry.getInstance().register(batchesTimer);
}

    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(action);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    public <T> T execute(StatementCallback<T> action) throws DataAccessException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(action);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    public void execute(String sql) throws DataAccessException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            delegate.execute(sql);
            success = true;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.query(sql, rse);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            delegate.query(sql, rch);
            success = true;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.query(sql, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, requiredType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public Map<String, Object> queryForMap(String sql) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            Map<String, Object> ret = delegate.queryForMap(sql);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.queryForList(sql, elementType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<Map<String, Object>> ret = delegate.queryForList(sql);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            SqlRowSet ret = delegate.queryForRowSet(sql);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.query(psc, rse);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T query(String sql, PreparedStatementSetter pss, ResultSetExtractor<T> rse)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.query(sql, pss, rse);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.query(sql, args, argTypes, rse);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.query(sql, args, rse);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.query(sql, rse, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            delegate.query(psc, rch);
            success = true;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            delegate.query(sql, pss, rch);
            success = true;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            delegate.query(sql, args, argTypes, rch);
            success = true;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public void query(String sql, Object[] args, RowCallbackHandler rch) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            delegate.query(sql, args, rch);
            success = true;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public void query(String sql, RowCallbackHandler rch, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            delegate.query(sql, rch, args);
            success = true;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.query(psc, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> query(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.query(sql, pss, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.query(sql, args, argTypes, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.query(sql, args, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.query(sql, rowMapper, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, args, argTypes, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, args, rowMapper);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, rowMapper, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, args, argTypes, requiredType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, Object[] args, Class<T> requiredType) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, args, requiredType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            T ret = delegate.queryForObject(sql, requiredType, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            Map<String, Object> ret = delegate.queryForMap(sql, args, argTypes);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            Map<String, Object> ret = delegate.queryForMap(sql, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.queryForList(sql, args, argTypes, elementType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> queryForList(String sql, Object[] args, Class<T> elementType) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.queryForList(sql, args, elementType);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<T> ret = delegate.queryForList(sql, elementType, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes)
            throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<Map<String, Object>> ret = delegate.queryForList(sql, args, argTypes);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            List<Map<String, Object>> ret = delegate.queryForList(sql, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            SqlRowSet ret = delegate.queryForRowSet(sql, args, argTypes);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public SqlRowSet queryForRowSet(String sql, Object... args) throws DataAccessException {
        queriesExecuted.increment();
        Stopwatch s = queriesTimer.start();
        boolean success = false;

        try{
            SqlRowSet ret = delegate.queryForRowSet(sql, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                queriesErrors.increment();
            }
        }
    }

    public int update(String sql) throws DataAccessException {
        updatesExecuted.increment();
        Stopwatch s = updatesTimer.start();
        boolean success = false;

        try{
            int ret = delegate.update(sql);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                updatesErrors.increment();
            }
        }
    }

    public int update(PreparedStatementCreator psc) throws DataAccessException {
        updatesExecuted.increment();
        Stopwatch s = updatesTimer.start();
        boolean success = false;

        try{
            int ret = delegate.update(psc);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                updatesErrors.increment();
            }
        }
    }

    public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException {
        updatesExecuted.increment();
        Stopwatch s = updatesTimer.start();
        boolean success = false;

        try{
            int ret = delegate.update(psc, generatedKeyHolder);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                updatesErrors.increment();
            }
        }
    }

    public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
        updatesExecuted.increment();
        Stopwatch s = updatesTimer.start();
        boolean success = false;

        try{
            int ret = delegate.update(sql, pss);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                updatesErrors.increment();
            }
        }
    }

    public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        updatesExecuted.increment();
        Stopwatch s = updatesTimer.start();
        boolean success = false;

        try{
            int ret = delegate.update(sql, args, argTypes);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                updatesErrors.increment();
            }
        }
    }

    public int update(String sql, Object... args) throws DataAccessException {
        updatesExecuted.increment();
        Stopwatch s = updatesTimer.start();
        boolean success = false;

        try{
            int ret = delegate.update(sql, args);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                updatesErrors.increment();
            }
        }
    }

    public int[] batchUpdate(String... sql) throws DataAccessException {
        batchesExecuted.increment();
        Stopwatch s = batchesTimer.start();
        boolean success = false;

        try{
            int[] ret = delegate.batchUpdate(sql);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                batchesErrors.increment();
            }
        }
    }
        
    public int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException {
        batchesExecuted.increment();
        Stopwatch s = batchesTimer.start();
        boolean success = false;

        try{
            int[] ret = delegate.batchUpdate(sql, pss);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                batchesErrors.increment();
            }
        }
    }

    public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
        batchesExecuted.increment();
        Stopwatch s = batchesTimer.start();
        boolean success = false;

        try{
            int[] ret = delegate.batchUpdate(sql, batchArgs);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                batchesErrors.increment();
            }
        }
    }

    public int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException {
        batchesExecuted.increment();
        Stopwatch s = batchesTimer.start();
        boolean success = false;

        try{
            int[] ret = delegate.batchUpdate(sql, batchArgs, argTypes);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                batchesErrors.increment();
            }
        }
    }

    public <T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
            ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {
        batchesExecuted.increment();
        Stopwatch s = batchesTimer.start();
        boolean success = false;

        try{
            int[][] ret = delegate.batchUpdate(sql, batchArgs, batchSize, pss);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                batchesErrors.increment();
            }
        }
    }

    public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action)
            throws DataAccessException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(csc, action);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
   }

    public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(callString, action);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
            throws DataAccessException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(psc, action);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }

    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
        execsExecuted.increment();
        Stopwatch s = execsTimer.start();
        boolean success = false;

        try{
            T ret = delegate.execute(sql, action);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                execsErrors.increment();
            }
        }
    }
        

    public Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
            throws DataAccessException {
        callsExecuted.increment();
        Stopwatch s = callsTimer.start();
        boolean success = false;

        try{
            Map<String, Object> ret = delegate.call(csc, declaredParameters);
            success = true;
            return ret;
        }finally{
            s.stop();
            if(!success){
                callsErrors.increment();
            }
        }
    }

    
    @Override
    public String toString() {
        return "JOWM-"+delegate.toString();
    }
    
}