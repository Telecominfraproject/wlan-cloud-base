package com.telecominfraproject.wlan.stream;

import java.util.List;

/**
 * @author dtop
 *
 */
public interface StreamConsumerInterface<T> {

	//work in progress
    List<T> poll();
}
