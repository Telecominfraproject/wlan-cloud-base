package com.telecominfraproject.wlan.core.server.jdbc.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
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
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
                    .addScript("classpath:schema-hsqldb-test.sql");

            builder.generateUniqueName(true);
            
            //
            // We only want to add the test-data.sql if the file actually
            // exists.
            //
            Set<String> testDataFiles = getReflections().getResources(Pattern.compile("test-data.sql"));

            if (!CollectionUtils.isEmpty(testDataFiles)) {
                builder.addScript("classpath:test-data.sql");
            }

            EmbeddedDatabase db = builder.build();
            return new BaseJdbcTestDatabase(db, new KeyColumnUpperCaseConverter());
        }

        public static Reflections getReflections() {
            //scan urls that contain 'com.telecominfraproject.wlan' and vendor-specific top level packages, use the ResourcesScanner

            List<URL> urls =  new ArrayList<>();
            urls.addAll(ClasspathHelper.forPackage("com.telecominfraproject.wlan"));
            
            //add vendor packages
            if(BaseJsonModel.vendorTopLevelPackages!=null) {
                String[] vendorPkgs = BaseJsonModel.vendorTopLevelPackages.split(",");
                for(int i=0; i< vendorPkgs.length; i++) {
                    if(vendorPkgs[i].trim().isEmpty()) {
                        continue;
                    }
                    
                    urls.addAll(ClasspathHelper.forPackage(vendorPkgs[i]));
                    
                }
            }
                    
            Reflections reflections =   new Reflections(new ConfigurationBuilder()
                    .setUrls(urls)
                    .setScanners(new ResourcesScanner() ));         

            return reflections;
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
