package com.telecominfraproject.wlan.core.server.async.example;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author dtop
 * 
 * This is an example of how to call methods that are meant to be called asynchronously (and marked with @Async annotations)
 *
 */
@Component
@Profile(value={"asyncExample"})
public class AsyncCallerExample {

   private static final Logger LOG = LoggerFactory.getLogger(AsyncCallerExample.class);

   @Autowired private AsyncExample asyncExample;
   
   public void exampleMethod() {
       LOG.debug("Running Async String Example on thread {}", Thread.currentThread().getName());
       Future<String> asyncRet = asyncExample.asyncStringExample();
       try {
           LOG.debug("Async Returned Value : {}", asyncRet.get());
       } catch (Exception e) {
           LOG.error("Async execution error", e);
       }
   
       LOG.debug("Running Async Void Example on thread {}", Thread.currentThread().getName());
       asyncExample.asyncVoidExample();        
   }
}
