package com.telecominfraproject.wlan.core.server.cassandra;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.datastax.dse.driver.api.core.cql.continuous.ContinuousAsyncResultSet;
import com.datastax.dse.driver.api.core.cql.continuous.ContinuousResultSet;
import com.datastax.dse.driver.api.core.cql.continuous.reactive.ContinuousReactiveResultSet;
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet;
import com.datastax.dse.driver.api.core.graph.AsyncGraphResultSet;
import com.datastax.dse.driver.api.core.graph.GraphResultSet;
import com.datastax.dse.driver.api.core.graph.GraphStatement;
import com.datastax.dse.driver.api.core.graph.reactive.ReactiveGraphResultSet;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.PrepareRequest;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metrics.Metrics;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;

public class CqlSessionWithMetrics implements CqlSession {

    private final TagList tags = CloudMetricsTags.commonTags;

    final Counter executeCounter = new BasicCounter(MonitorConfig.builder("cassandra-execute").withTags(tags).build());
    final Counter executeAsyncCounter = new BasicCounter(MonitorConfig.builder("cassandra-execute-async").withTags(tags).build());
    final Counter executeReactiveCounter = new BasicCounter(MonitorConfig.builder("cassandra-execute-reactive").withTags(tags).build());

    final Counter executeErrorCounter = new BasicCounter(MonitorConfig.builder("cassandra-execute-errors").withTags(tags).build());
    final Counter executeAsyncErrorCounter = new BasicCounter(MonitorConfig.builder("cassandra-execute-async-errors").withTags(tags).build());
    final Counter executeReactiveErrorCounter = new BasicCounter(MonitorConfig.builder("cassandra-execute-reactive-errors").withTags(tags).build());

    private final Timer executeTimer = new BasicTimer(
            MonitorConfig.builder("cassandra-executeTimer").withTags(tags).build());

    private final Timer executeAsyncTimer = new BasicTimer(
            MonitorConfig.builder("cassandra-executeAsyncTimer").withTags(tags).build());

    private final Timer executeReactiveTimer = new BasicTimer(
            MonitorConfig.builder("cassandra-executeReactiveTimer").withTags(tags).build());

    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(executeCounter);
        DefaultMonitorRegistry.getInstance().register(executeAsyncCounter);
        DefaultMonitorRegistry.getInstance().register(executeReactiveCounter);
        
        DefaultMonitorRegistry.getInstance().register(executeErrorCounter);
        DefaultMonitorRegistry.getInstance().register(executeAsyncErrorCounter);
        DefaultMonitorRegistry.getInstance().register(executeReactiveErrorCounter);

        DefaultMonitorRegistry.getInstance().register(executeTimer);
        DefaultMonitorRegistry.getInstance().register(executeAsyncTimer);
        DefaultMonitorRegistry.getInstance().register(executeReactiveTimer);
    }

    private final CqlSession delegate;
    
    public CqlSessionWithMetrics(CqlSession delegate) {
        this.delegate = delegate;
    }

    public CompletionStage<AsyncResultSet> executeAsync(Statement<?> statement) {
        executeAsyncCounter.increment();
        Stopwatch stopwatch = executeAsyncTimer.start();
        boolean success = false;
        
        try {
            CompletionStage<AsyncResultSet> ret = delegate.executeAsync(statement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeAsyncErrorCounter.increment();
            }
        }
        
    }

    public ReactiveGraphResultSet executeReactive(GraphStatement<?> statement) {
        executeReactiveCounter.increment();
        Stopwatch stopwatch = executeReactiveTimer.start();
        boolean success = false;
        
        try {
            ReactiveGraphResultSet ret = delegate.executeReactive(statement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeReactiveErrorCounter.increment();
            }
        }
            
    }

    public CompletionStage<Void> closeFuture() {
        return delegate.closeFuture();
    }

    public ResultSet execute(Statement<?> statement) {
        executeCounter.increment();
        Stopwatch stopwatch = executeTimer.start();
        boolean success = false;
        
        try {
            ResultSet ret = delegate.execute(statement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeErrorCounter.increment();
            }
        }
        
    }

    public GraphResultSet execute(GraphStatement<?> graphStatement) {
        executeCounter.increment();
        Stopwatch stopwatch = executeTimer.start();
        boolean success = false;
        
        try {
            GraphResultSet ret = delegate.execute(graphStatement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeErrorCounter.increment();
            }
        }

    }

    public boolean isClosed() {
        return delegate.isClosed();
    }

    public ReactiveResultSet executeReactive(String query) {
        executeReactiveCounter.increment();
        Stopwatch stopwatch = executeReactiveTimer.start();
        boolean success = false;
        
        try {
            ReactiveResultSet ret = delegate.executeReactive(query);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeReactiveErrorCounter.increment();
            }
        }
        
    }

    public CompletionStage<AsyncResultSet> executeAsync(String query) {
        executeAsyncCounter.increment();
        Stopwatch stopwatch = executeAsyncTimer.start();
        boolean success = false;
        
        try {
            CompletionStage<AsyncResultSet> ret = delegate.executeAsync(query);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeAsyncErrorCounter.increment();
            }
        }        
        
    }

    public CompletionStage<Void> closeAsync() {
        return delegate.closeAsync();
    }

    public ReactiveResultSet executeReactive(Statement<?> statement) {
        executeReactiveCounter.increment();
        Stopwatch stopwatch = executeReactiveTimer.start();
        boolean success = false;
        
        try {
            ReactiveResultSet ret = delegate.executeReactive(statement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeReactiveErrorCounter.increment();
            }
        }
        
    }

    public CompletionStage<Void> forceCloseAsync() {
        return delegate.forceCloseAsync();
    }

    public CompletionStage<PreparedStatement> prepareAsync(SimpleStatement statement) {
        return delegate.prepareAsync(statement);
    }

    public ContinuousReactiveResultSet executeContinuouslyReactive(String query) {       
        executeReactiveCounter.increment();
        Stopwatch stopwatch = executeReactiveTimer.start();
        boolean success = false;
        
        try {
            ContinuousReactiveResultSet ret = delegate.executeContinuouslyReactive(query);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeReactiveErrorCounter.increment();
            }
        }        
    }

    public ResultSet execute(String query) {
        executeCounter.increment();
        Stopwatch stopwatch = executeTimer.start();
        boolean success = false;
        
        try {
            ResultSet ret = delegate.execute(query);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeErrorCounter.increment();
            }
        }

    }

    public void close() {
        delegate.close();
    }

    public String getName() {
        return delegate.getName();
    }

    public CompletionStage<AsyncGraphResultSet> executeAsync(GraphStatement<?> graphStatement) {
        executeAsyncCounter.increment();
        Stopwatch stopwatch = executeAsyncTimer.start();
        boolean success = false;
        
        try {
            CompletionStage<AsyncGraphResultSet> ret = delegate.executeAsync(graphStatement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeAsyncErrorCounter.increment();
            }
        }
        
    }

    public ContinuousReactiveResultSet executeContinuouslyReactive(Statement<?> statement) {
        executeReactiveCounter.increment();
        Stopwatch stopwatch = executeReactiveTimer.start();
        boolean success = false;
        
        try {
            ContinuousReactiveResultSet ret = delegate.executeContinuouslyReactive(statement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeReactiveErrorCounter.increment();
            }
        }        
        
    }

    public CompletionStage<PreparedStatement> prepareAsync(String query) {
        return delegate.prepareAsync(query);
    }

    public PreparedStatement prepare(SimpleStatement statement) {
        return delegate.prepare(statement);
    }

    public Metadata getMetadata() {
        return delegate.getMetadata();
    }

    public ContinuousResultSet executeContinuously(Statement<?> statement) {
        executeCounter.increment();
        Stopwatch stopwatch = executeTimer.start();
        boolean success = false;
        
        try {
            ContinuousResultSet ret = delegate.executeContinuously(statement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeErrorCounter.increment();
            }
        }
        
    }

    public CompletionStage<PreparedStatement> prepareAsync(PrepareRequest request) {
        return delegate.prepareAsync(request);
    }

    public boolean isSchemaMetadataEnabled() {
        return delegate.isSchemaMetadataEnabled();
    }

    public CompletionStage<ContinuousAsyncResultSet> executeContinuouslyAsync(Statement<?> statement) {
        executeAsyncCounter.increment();
        Stopwatch stopwatch = executeAsyncTimer.start();
        boolean success = false;
        
        try {
            CompletionStage<ContinuousAsyncResultSet> ret = delegate.executeContinuouslyAsync(statement);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeAsyncErrorCounter.increment();
            }
        }
        
    }

    public CompletionStage<Metadata> setSchemaMetadataEnabled(Boolean newValue) {
        return delegate.setSchemaMetadataEnabled(newValue);
    }

    public CompletionStage<Metadata> refreshSchemaAsync() {
        return delegate.refreshSchemaAsync();
    }

    public Metadata refreshSchema() {
        return delegate.refreshSchema();
    }

    public CompletionStage<Boolean> checkSchemaAgreementAsync() {
        return delegate.checkSchemaAgreementAsync();
    }

    public PreparedStatement prepare(String query) {
        return delegate.prepare(query);
    }

    public boolean checkSchemaAgreement() {
        return delegate.checkSchemaAgreement();
    }

    public DriverContext getContext() {
        return delegate.getContext();
    }

    public Optional<CqlIdentifier> getKeyspace() {
        return delegate.getKeyspace();
    }

    public PreparedStatement prepare(PrepareRequest request) {
        return delegate.prepare(request);
    }

    public Optional<Metrics> getMetrics() {
        return delegate.getMetrics();
    }

    public <RequestT extends Request, ResultT> ResultT execute(RequestT request, GenericType<ResultT> resultType) {
        executeCounter.increment();
        Stopwatch stopwatch = executeTimer.start();
        boolean success = false;
        
        try {
            ResultT ret = delegate.execute(request, resultType);
            success = true;
            return ret;
        } finally {
            stopwatch.stop();
            if(!success) {
                executeErrorCounter.increment();
            }
        }

    }    

}
