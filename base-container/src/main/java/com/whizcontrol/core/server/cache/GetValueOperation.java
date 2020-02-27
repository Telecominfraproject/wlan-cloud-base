/**
 * 
 */
package com.whizcontrol.core.server.cache;

/**
 * @author yongli
 *
 */
public interface GetValueOperation<K, V> {
    V getRecord(K key);
}
