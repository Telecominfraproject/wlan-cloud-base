package com.telecominfraproject.wlan.core.server.tx.test;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author dtop
 *
 */
@Configuration
public class TxTestConfig {

    private static final Logger LOG = LoggerFactory.getLogger(TxTestConfig.class);

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        PlatformTransactionManager ptm = new PlatformTransactionManager() {
            private ThreadLocal<List<TransactionStatus>> currentTx = new ThreadLocal<>() ;
            
            {
                LOG.info("*** Using simulated PlatformTransactionManager");
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                LOG.debug("Simulating Rollback for {}", status);
                if(currentTx.get().size() == 1 ) {
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        TransactionSynchronizationManager.clearSynchronization();
                    }
                    currentTx.remove();
                } else {
                    currentTx.get().remove(currentTx.get().size() - 1 );
                }
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                LOG.debug("Simulating Commit for {}", status);
                if(currentTx.get().size() == 1 ) {
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager
                                .getSynchronizations();
                        if (synchronizations != null) {
                            for (TransactionSynchronization synchronization : synchronizations) {
                                synchronization.afterCommit();
                            }
                        }
    
                        TransactionSynchronizationManager.clearSynchronization();
                    }
                    
                    currentTx.remove();
                } else {
                    currentTx.get().remove(currentTx.get().size() - 1 );
                }
            }
            
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                LOG.debug("Simulating getTransaction for {}", definition);
                if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.initSynchronization();
                }
                
                if (currentTx.get() == null) {
                    List<TransactionStatus> txList = new ArrayList<>();
                    TransactionStatus ts = new SimpleTransactionStatus(true);
                    txList.add(ts);
                    currentTx.set(txList);
                    return ts;
                } else {
                    List<TransactionStatus> txList = currentTx.get();
                    TransactionStatus ts = new SimpleTransactionStatus(false);
                    txList.add(ts);
                    return ts;
                }
            }

        };
        return ptm;
    }

}
