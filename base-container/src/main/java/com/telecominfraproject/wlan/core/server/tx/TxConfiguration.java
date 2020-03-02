package com.telecominfraproject.wlan.core.server.tx;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TxConfiguration { 
    //dtop: uncomment some of the code below when we outgrow capabilities provided by autoconfigured transaction manager
    //implements TransactionManagementConfigurer 

    //dtop:
    // Datasources in our projects are defined outside of this class
    // When using DataSourceTransactionManager it makes sense to define one data source with SingleDataSourceConfig
    // When dealing with truly required multiple data sources that have to be coordinated with 2-phase-commit - JtaTransactionManager can be used  
    //
//    @Bean
//    public DataSource dataSource() {
//        // configure and return the necessary JDBC DataSource
//    }

    //dtop: this DataSourceTransactionManager will be auto-configured by DataSourceTransactionManagerAutoConfiguration if not defined in here
    //
//    @Bean(name={"transactionManager"})
//    @Profile("use_single_ds")
//    public PlatformTransactionManager txManager(DataSource dataSource) {
//        return new DataSourceTransactionManager(dataSource);
//    }

    //dtop: enable this after clearly identifying the need for the 2pc usecase
    //for now dealing with defaulkt auto-configured DataSourceTransactionManager should be enough 
    //
//    @Bean(name={"transactionManager"})
//    @Profile("use_2pc_tx")
//    public PlatformTransactionManager txManager() {
//        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
//        UserTransaction userTransaction = ...;
//        jtaTransactionManager.setUserTransaction(userTransaction );
//        TransactionManager transactionManager = ...;
//        jtaTransactionManager.setTransactionManager(transactionManager);
//        jtaTransactionManager.afterPropertiesSet();
//        return jtaTransactionManager;
//    }

    //dtop: this method is specified in TransactionManagementConfigurer, and can be used to designate one of the existing transactionManagers to handle @Transactional annotations
    // without TransactionManagementConfigurer (and without this method), @Transactional annotations will be processed by the auto-configured transactionManager 
//    @Override
//    public PlatformTransactionManager annotationDrivenTransactionManager() {
//        return txManager();
//    } 
    
}
