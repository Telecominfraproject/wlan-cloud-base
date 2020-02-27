package com.whizcontrol.core.server.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.google.common.cache.CacheBuilderSpec;

@Configuration
public class CacheConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(CacheConfiguration.class);
    
    @Autowired
    ApplicationContext applicationContext;
    
    @Bean
    public CacheManager cacheManager(Environment env){
        
        GuavaCacheManager gcm = new GuavaCacheManager();
        
        String cacheBuilderSpecification = env.getProperty("whizcontrol.cacheBuilderSpecification", "concurrencyLevel=4,maximumSize=10000,expireAfterWrite=10m");
        LOG.info("configuring cache manager with '{}'", cacheBuilderSpecification);
        
        CacheBuilderSpec cacheBuilderSpec = CacheBuilderSpec.parse(cacheBuilderSpecification ) ;
        gcm.setCacheBuilderSpec(cacheBuilderSpec);
        
        return gcm;
    } 
    
    public Cache getCache(CacheType cacheType, String cacheName){
        CacheManager cm = applicationContext.getBean(CacheManager.class, cacheType.toString());
        return cm.getCache(cacheName);
    }
    
    @Bean
    public CacheManager cacheManagerShortLived(Environment env) {
        GuavaCacheManager gcm = new GuavaCacheManager();
        
        String cacheBuilderSpecification = env.getProperty("whizcontrol.cacheBuilderSpecification.shortLived", "concurrencyLevel=4,maximumSize=10000,expireAfterWrite=1m");
        LOG.info("configuring short-lived cache manager with '{}'", cacheBuilderSpecification);
        
        CacheBuilderSpec cacheBuilderSpec = CacheBuilderSpec.parse(cacheBuilderSpecification ) ;
        gcm.setCacheBuilderSpec(cacheBuilderSpec);
        
        return gcm;
        
    }
}
