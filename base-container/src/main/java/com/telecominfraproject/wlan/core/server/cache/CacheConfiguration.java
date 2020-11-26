package com.telecominfraproject.wlan.core.server.cache;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Primary;

@Configuration
public class CacheConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(CacheConfiguration.class);

    @Autowired
	AnnotationConfigApplicationContext applicationContext;

    @Bean
	@Primary
	@Qualifier("cacheManager")
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
//    	   .weakKeys()
    	   .recordStats();
    }

    @Bean
	@Qualifier("cacheManagerShortLived")
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
//    	   .weakKeys()
    	   .recordStats();
    }

	@Bean
	@Qualifier("cacheManagerLongLived")
	public CacheManager cacheManagerLongLived() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		Caffeine<Object, Object> cb = caffeineCacheBuilderLongLived();
		cacheManager.setCaffeine(cb);
		LOG.info("Configured cache manager with expiry {}", cb);

		return cacheManager;
	}

	Caffeine<Object, Object> caffeineCacheBuilderLongLived() {
		return Caffeine.newBuilder()
				.initialCapacity(100)
				.maximumSize(10000)
				.expireAfterAccess(2, TimeUnit.HOURS)
//              .weakKeys() Do not use weak keys - garbage collection will remove our values when we still want them
				.recordStats();
	}

	public Cache getCache(String cacheName, String cacheManagerQualifier) {
		CacheManager cm = BeanFactoryAnnotationUtils.qualifiedBeanOfType(applicationContext.getBeanFactory(),
				CacheManager.class, cacheManagerQualifier);
		return cm.getCache(cacheName);

	}
}
