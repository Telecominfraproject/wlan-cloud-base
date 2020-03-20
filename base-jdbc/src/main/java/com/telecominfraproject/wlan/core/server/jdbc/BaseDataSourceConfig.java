package com.telecominfraproject.wlan.core.server.jdbc;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

public abstract class BaseDataSourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(BaseDataSourceConfig.class);

    private final TagList tags = CloudMetricsTags.commonTags;

    @Autowired
    private Environment environment;

    @Monitor(name = "getConnection", type = DataSourceType.COUNTER)
    private final AtomicInteger getConnectionExecuted = new AtomicInteger(0);

    static interface DataSourceInSpringClassloaderInterface extends DataSource {
    }

    class DataSourceInSpringClassloader implements DataSourceInSpringClassloaderInterface {
        DataSource dataSource;
        String id;

        public DataSourceInSpringClassloader(String datasourceId, DataSource dataSource) {
            this.dataSource = dataSource;
            this.id = datasourceId;
        }

        public PrintWriter getLogWriter() throws SQLException {
            return dataSource.getLogWriter();
        }

        public <T> T unwrap(Class<T> iface) throws SQLException {
            return dataSource.unwrap(iface);
        }

        public void setLogWriter(PrintWriter out) throws SQLException {
            dataSource.setLogWriter(out);
        }

        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return dataSource.isWrapperFor(iface);
        }

        public Connection getConnection() throws SQLException {
            getConnectionExecuted.incrementAndGet();
            return dataSource.getConnection();
        }

        public void setLoginTimeout(int seconds) throws SQLException {
            dataSource.setLoginTimeout(seconds);
        }

        public Connection getConnection(String username, String password) throws SQLException {
            getConnectionExecuted.incrementAndGet();
            return dataSource.getConnection(username, password);
        }

        public int getLoginTimeout() throws SQLException {
            return dataSource.getLoginTimeout();
        }

        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return dataSource.getParentLogger();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public DataSource getDataSource() {
        Properties dataSourceProperties = getDataSourceProperties();
        DriverAdapterCPDS driverAdapterCPDS = new DriverAdapterCPDS();
        driverAdapterCPDS.setUrl(dataSourceProperties.getProperty("url"));
        driverAdapterCPDS.setUser(dataSourceProperties.getProperty("username"));
        driverAdapterCPDS.setPassword(dataSourceProperties.getProperty("password"));

        try {
            driverAdapterCPDS.setDriver(dataSourceProperties.getProperty("driverClass"));
        } catch (Exception e) {
            throw new ConfigurationException("Failed to set driver for data source", e);
        }

        driverAdapterCPDS
                .setMaxPreparedStatements(Integer.valueOf(dataSourceProperties.getProperty("maxPreparedStatements")));
        driverAdapterCPDS.setMaxIdle(Integer.valueOf(dataSourceProperties.getProperty("maxIdlePreparedStatements")));
        driverAdapterCPDS
                .setPoolPreparedStatements(Boolean.valueOf(dataSourceProperties.getProperty("poolPreparedStatements")));

        final SharedPoolDataSource poolDataSource = new SharedPoolDataSource();
        poolDataSource.setDefaultMaxIdle(Integer.valueOf(dataSourceProperties.getProperty("maxIdleConnections", "8")));
        poolDataSource
                .setDefaultMaxTotal(Integer.valueOf(dataSourceProperties.getProperty("maxTotalConnections", "8")));
        poolDataSource.setConnectionPoolDataSource(driverAdapterCPDS);
        poolDataSource.setDefaultMaxWaitMillis(Integer.valueOf(dataSourceProperties.getProperty("maxWaitMs")));
        poolDataSource.setDefaultTransactionIsolation(
                Integer.valueOf(dataSourceProperties.getProperty("defaultTransactionIsolation")));
        poolDataSource.setDefaultReadOnly(Boolean.valueOf(dataSourceProperties.getProperty("defaultReadOnly")));
        poolDataSource.setDefaultTestOnCreate(Boolean.valueOf(dataSourceProperties.getProperty("testOnCreate")));
        poolDataSource.setDefaultTestOnBorrow(Boolean.valueOf(dataSourceProperties.getProperty("testOnBorrow")));
        poolDataSource.setDefaultTestOnReturn(Boolean.valueOf(dataSourceProperties.getProperty("testOnReturn")));
        poolDataSource.setDefaultTestWhileIdle(Boolean.valueOf(dataSourceProperties.getProperty("testWhileIdle")));
        poolDataSource.setValidationQuery("SELECT 0");

        // //wrap original datasource so that TimedInterface.newProxy picks up
        // correct classloader
        String datasourceId = getDataSourceName();
        DataSourceInSpringClassloader wrappedObj = new DataSourceInSpringClassloader(datasourceId, poolDataSource);

        Monitors.registerObject(datasourceId, this);

        BasicGauge<Integer> numberOfActiveJDBCConnections = new BasicGauge<>(
                MonitorConfig.builder(getDataSourceName() + "-numberOfActiveJDBCConnections").withTags(tags).build(),
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return poolDataSource.getNumActive();
                    }
                });

        DefaultMonitorRegistry.getInstance().register(numberOfActiveJDBCConnections);

        BasicGauge<Integer> numberOfIdleJDBCConnections = new BasicGauge<>(
                MonitorConfig.builder(getDataSourceName() + "-numberOfIdleJDBCConnections").withTags(tags).build(),
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return poolDataSource.getNumIdle();
                    }
                });

        DefaultMonitorRegistry.getInstance().register(numberOfIdleJDBCConnections);

        return wrappedObj;
        // //wrap and register this object to produce JMX metrics
        // DataSource ret =
        // TimedInterface.newProxy(DataSourceInSpringClassloaderInterface.class,
        // wrappedObj, "JDBCDatasourcePool");
        // DefaultMonitorRegistry.getInstance().register((CompositeMonitor)ret);
        //
        // return ret;
    }

    public abstract String getDataSourceName();

    /**
     * Get the Key Column Converter base on setting
     * 
     * @return the resulting converter
     */
    public BaseKeyColumnConverter getKeyColumnConverter() {
        Properties dataSourceProperties = getDataSourceProperties();
        String name = dataSourceProperties.getProperty("keyColConversionClass");
        try {
            if (null != name) {
                Class<?> clazz = Class.forName(name);
                Constructor<?> constructor = clazz.getConstructor();
                return (BaseKeyColumnConverter) constructor.newInstance();
            } else {
                return new KeyColumnConverter();
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new ConfigurationException("Failed to set up keyColConversionClass for datasource", e);
        }
    }

    public Properties getDataSourceProperties() {

        Properties p = new Properties();

        p.setProperty("url", environment.getProperty(getDataSourceName() + ".url",
                "jdbc:postgresql://postgres-test-instance.crwckwjetrxv.us-east-1.rds.amazonaws.com:5432/testdb"));
        p.setProperty("username", environment.getProperty(getDataSourceName() + ".username", "testdb"));

        p.setProperty("driverClass",
                environment.getProperty(getDataSourceName() + ".driverClass", "org.postgresql.Driver"));

        p.setProperty("maxTotalConnections",
                environment.getProperty(getDataSourceName() + ".maxTotalConnections", "8"));
        p.setProperty("maxIdleConnections", environment.getProperty(getDataSourceName() + ".maxIdleConnections", "8"));

        p.setProperty("maxPreparedStatements",
                environment.getProperty(getDataSourceName() + ".maxPreparedStatements", "200"));
        p.setProperty("maxIdlePreparedStatements",
                environment.getProperty(getDataSourceName() + ".maxIdlePreparedStatements", "200"));
        p.setProperty("poolPreparedStatements",
                environment.getProperty(getDataSourceName() + ".poolPreparedStatements", "true"));

        p.setProperty("maxWaitMs", environment.getProperty(getDataSourceName() + ".maxWaitMs", "1000"));
        p.setProperty("defaultTransactionIsolation",
                environment.getProperty(getDataSourceName() + ".defaultTransactionIsolation",
                        String.valueOf(Connection.TRANSACTION_READ_COMMITTED)));
        p.setProperty("defaultReadOnly", environment.getProperty(getDataSourceName() + ".defaultReadOnly", "false"));
        p.setProperty("testOnCreate", environment.getProperty(getDataSourceName() + ".testOnCreate", "true"));
        p.setProperty("testOnBorrow", environment.getProperty(getDataSourceName() + ".testOnBorrow", "true"));
        p.setProperty("testOnReturn", environment.getProperty(getDataSourceName() + ".testOnReturn", "true"));
        p.setProperty("testWhileIdle", environment.getProperty(getDataSourceName() + ".testWhileIdle", "true"));
        p.setProperty("keyColConversionClass", environment.getProperty(getDataSourceName() + ".keyColConversionClass",
                "com.telecominfraproject.wlan.core.server.jdbc.KeyColumnLowerCaseConverter"));
        String password = environment.getProperty(getDataSourceName() + ".password", "testdb");
        p.setProperty("passwordHash", DigestUtils.sha256Hex(password));
        LOG.info("Loaded properties for {} datasource from {}: {}", getDataSourceName(),
                environment.getProperty(getDataSourceName() + ".props"), p);
        // not logging password
        p.setProperty("password", password);
        return p;
    }
}
