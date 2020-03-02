/**
 * 
 */
package com.telecominfraproject.wlan.core.server.cache;

/**
 * @author yongli
 *
 */
public interface GetValueOperation<K, V> {
    V getRecord(K key);
}
