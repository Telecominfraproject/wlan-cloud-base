package com.whizcontrol.core.server.async.example;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

/**
 * @author dtop
 * 
 * This is an example of how to create methods that are meant to be called asynchronously
 *
 */
@Component
@Profile(value={"asyncExample"})
public class AsyncExample {

        private static final Logger LOG = LoggerFactory.getLogger(AsyncExample.class);

        /**
         * Example of async method that returns a value.
         * @return
         */
        @Async
        public Future<String> asyncStringExample(){
                LOG.debug("** Executing asyncStringExample on thread {}", Thread.currentThread().getName());
                return new AsyncResult<>("return from async method");
        }

        /**
         * Example of async method that does not return any value
         */
        @Async
        public void asyncVoidExample(){
                LOG.debug("** Executing asyncVoidExample on thread {}", Thread.currentThread().getName());
        }

        
        /**
         * Example of async method that will be processed by a particular Executor.
         * Name in this @Async annotation has to match name of the bean that configures Executor, like it is done in AsyncConfiguration
         * 
         * @see AsyncConfiguration
         */
        @Async("asyncExecutor")
        public void asyncVoidExampleOnASpecificExecutor(){
                LOG.debug("** Executing asyncVoidExample on thread {}", Thread.currentThread().getName());
        }

}
