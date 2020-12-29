package com.telecominfraproject.wlan.core.server.jdbc.test;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import com.telecominfraproject.wlan.core.server.jdbc.BaseJDbcDataSource;
import com.telecominfraproject.wlan.core.server.jdbc.BaseKeyColumnConverter;
import com.telecominfraproject.wlan.core.server.jdbc.KeyColumnUpperCaseConverter;

/**
 * Base classes for JDBC DAOs. Note that all tests will be run within
 * transaction (one tx per test method), and all the db changes <b>will be
 * rolled back</b> at the end of the transaction.
 * 
 * <p>
 * When executing transactional tests, it is sometimes useful to be able to
 * execute certain <em>set up</em> or <em>tear down</em> code outside of a
 * transaction. This can be achieved by annotating methods with
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction}.
 * 
 * <pre>
 * <code>
 * &#64;Import(value = { TestConfiguration.class })
 * &#64;TestWithEmbeddedDB
 * </code>
 * </pre>
 * 
 * @author dtop
 * @author yongli
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = BaseJdbcTest.Config.class)
@Rollback(value = true)
@Transactional
public abstract class BaseJdbcTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseJdbcTest.class);

    @Autowired(required = false)
    protected EmbeddedDatabase db;

    public static class BaseJdbcTestDatabase extends BaseJDbcDataSource implements EmbeddedDatabase {

        public BaseJdbcTestDatabase(EmbeddedDatabase targetDataSource, BaseKeyColumnConverter targetConverter) {
            super(targetDataSource, targetConverter);
        }

        @Override
        public void shutdown() {
            EmbeddedDatabase db = (EmbeddedDatabase) getTargetDataSource();
            if (db != null) {
                db.shutdown();
            }
        }
    }

    @Configuration
    // @PropertySource({ "classpath:persistence-${envTarget:dev}.properties" })
    public static class Config {
        // Put all required @Bean -s in here - they will be injected into the
        // AppIntegrationTest class

        @Bean
        @Profile("use_embedded_db")
        @Primary
        EmbeddedDatabase getEmbeddedDatabase() {
            // creates a HSQL in-memory db populated from scripts
            // classpath:schema-hsqldb-test.sql and classpath:test-data.sql
            // this will auto-wire DataSource object
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();

            // If the EmbeddedDatabaseBuilder class had a following method, we would have used it directly:
            //            public EmbeddedDatabaseBuilder addScript(Resource scriptResource) {
            //                this.databasePopulator.addScript(scriptResource);
            //                return this;
            //            }
            // But it does not have this method, and we will have to use reflection to achieve similar effect.
            //
            // We need to expose ResourceDatabasePopulator databasePopulator field of the
            // EmbeddedDatabaseBuilder to be able to pass to it resources with the same name
            // in the classpath, i.e. schema-hsqldb-test.sql files that are defined in
            // different modules
            ResourceDatabasePopulator dbPopulator;
            try {
                Field dbPopulatorField = EmbeddedDatabaseBuilder.class.getDeclaredField("databasePopulator");
                //the field is private, so we have to use a hammer
                dbPopulatorField.setAccessible(true);
                dbPopulator = (ResourceDatabasePopulator) dbPopulatorField.get(builder);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                throw new RuntimeException("Cannot get access to dbPopulator of the EmbeddedDatabaseBuilder : " + e.getMessage(), e);
            }  
            
            //
            // Add the schema-hsqldb-test.sql scripts to the DB builder
            //
            try {
                PathMatchingResourcePatternResolver pRes = new PathMatchingResourcePatternResolver();
                Resource[] resources = pRes.getResources("classpath*:schema-hsqldb-test.sql");
                if(resources!=null) {
                    for(Resource r: resources) {
                        dbPopulator.addScript(r);
                        LOG.debug("Adding SQL script {} - {}", r.getFilename(), r.getURI());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Cannot load SQL scripts: " + e.getMessage(), e);
            }


            builder.generateUniqueName(true);
            
            //
            // Now add the test-data.sql scripts to the DB builder
            //
            try {
                PathMatchingResourcePatternResolver pRes = new PathMatchingResourcePatternResolver();
                Resource[] resources = pRes.getResources("classpath*:test-data.sql");
                if(resources!=null) {
                    for(Resource r: resources) {
                        dbPopulator.addScript(r);
                        LOG.debug("Adding SQL script {} - {}", r.getFilename(), r.getURI());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Cannot load SQL scripts: ", e);
            }


            EmbeddedDatabase db = builder.build();
            
            return new BaseJdbcTestDatabase(db, new KeyColumnUpperCaseConverter());
        }

        @Bean
        @Primary
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

    }

    @BeforeTransaction
    public void beforeTx() {
        LOG.debug("*** before Tx");
    }

    @AfterTransaction
    public void afterTx() {
        LOG.debug("*** after Tx");
    }

}
