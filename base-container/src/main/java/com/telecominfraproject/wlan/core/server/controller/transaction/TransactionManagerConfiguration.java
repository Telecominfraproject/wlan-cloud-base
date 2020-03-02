/**
 * 
 */
package com.telecominfraproject.wlan.core.server.controller.transaction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provide a PseudoTransactionManager when base JDBC is not part of the process.
 * 
 * @author yongli
 *
 */
@Configuration
public class TransactionManagerConfiguration {

    @Bean
    @ConditionalOnMissingClass(value = "com.telecominfraproject.wlan.core.server.jdbc.BaseDataSourceConfig")
    public PlatformTransactionManager platformTransactionManager() {
        return new PseudoTransactionManager();
    }
}
