package com.telecominfraproject.wlan.stream;

import java.util.List;

/**
 * @author ekeddy
 * @author dtop
 *
 */
public interface StreamInterface<T> {

    void publish(T record);

    default void publish(List<T> records) {
    	if(records!=null) {
    		records.forEach(r -> publish(r));
    	}
    }

}
