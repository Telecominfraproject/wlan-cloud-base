/**
 * 
 */
package com.whizcontrol.core.server.cache;

/**
 * @author yongli
 *
 */
public class CacheResult<T> {
    /**
     * data found
     */
    private final T data;
    /**
     * Data was loaded from cache
     */
    private final boolean cached;

    public CacheResult(T data, boolean cached) {
        this.data = data;
        this.cached = cached;
    }

    public T getData() {
        return data;
    }

    public boolean isCached() {
        return cached;
    }
}
