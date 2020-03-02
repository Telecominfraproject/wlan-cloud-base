/**
 * 
 */
package com.telecominfraproject.wlan.core.server.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

/**
 * @author yongli
 *
 */
public abstract class BaseCache<K, D, O extends GetValueOperation<K, D>> {
    private final Cache dataCache;

    private final Class<? extends D> dataClazz;

    public BaseCache(final Cache dataCache, Class<? extends D> dataClazz) {
        this.dataCache = dataCache;
        this.dataClazz = dataClazz;
    }

    /**
     * Clear all cached value
     */
    public void clearCache() {
        this.dataCache.clear();
    }

    /**
     * Evict the current cached value
     * 
     * @param key
     */
    public void evictCacheData(K key) {
        if (null != key) {
            this.dataCache.evict(key);
        }
    }

    /**
     * Get the value either from cache of using the getOperation.
     * 
     * @param key
     * @param getOperation
     * @param forceUpdate
     * @return
     */
    public CacheResult<D> getData(K key, O getOperation, boolean forceUpdate) {
        if (null == key) {
            return new CacheResult<>(null, false);
        }
        if (!forceUpdate) {
            ValueWrapper cacheValue = this.dataCache.get(key);
            if (null != cacheValue) {
                Object value = cacheValue.get();
                if (null == value) {
                    return new CacheResult<>(null, true);
                }
                if (dataClazz.isInstance(value)) {
                    return new CacheResult<>(dataClazz.cast(value), true);
                }
                evictCacheData(key);
            }
        }
        D result = getOperation.getRecord(key);
        updateCacheData(key, result);
        return new CacheResult<>(result, false);
    }

    /**
     * Update cached data.
     * 
     * Support null value.
     * 
     * @param key
     * @param data
     */
    public void updateCacheData(K key, D data) {
        if (null != key) {
            this.dataCache.put(key, data);
        }
    }
}
