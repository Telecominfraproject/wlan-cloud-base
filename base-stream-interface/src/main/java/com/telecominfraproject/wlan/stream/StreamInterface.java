package com.telecominfraproject.wlan.stream;


/**
 * @author ekeddy
 *
 */
public interface StreamInterface<T> {

    void publish(T record);
}
