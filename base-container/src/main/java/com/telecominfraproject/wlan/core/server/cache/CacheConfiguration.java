package com.telecominfraproject.wlan.core.server.cache;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CacheConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(CacheConfiguration.class);
    
    @Autowired
    ApplicationContext applicationContext;
    
    @Bean
    public CacheManager cacheManager() {
    	  CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    	  Caffeine < Object, Object > cb = caffeineCacheBuilder();
    	  cacheManager.setCaffeine(cb);
    	  LOG.info("Configured cache manager {}",  cb);
    	  
    	  return cacheManager;
    }

    Caffeine < Object, Object > caffeineCacheBuilder() {
    	  return Caffeine.newBuilder()
    	   .initialCapacity(100)
    	   .maximumSize(10000)
    	   .expireAfterAccess(10, TimeUnit.MINUTES)
    	   .weakKeys()
    	   .recordStats();
    }    
    
    public Cache getCache(CacheType cacheType, String cacheName){
        CacheManager cm = applicationContext.getBean(CacheManager.class, cacheType.toString());
        return cm.getCache(cacheName);
    }
    
    
    @Bean
    public CacheManager cacheManagerShortLived() {
    	  CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    	  Caffeine < Object, Object > cb = caffeineCacheBuilderShortLived(); 
    	  cacheManager.setCaffeine(cb);
    	  LOG.info("Configured short-lived cache manager {}",  cb);

    	  return cacheManager;
    }

    Caffeine < Object, Object > caffeineCacheBuilderShortLived() {
    	  return Caffeine.newBuilder()
    	   .initialCapacity(100)
    	   .maximumSize(10000)
    	   .expireAfterAccess(1, TimeUnit.MINUTES)
    	   .weakKeys()
    	   .recordStats();
    }    

}
