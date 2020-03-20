package com.telecominfraproject.wlan.hazelcast.common;

import java.util.Map.Entry;

import com.hazelcast.query.Predicate;

public class SamplePredicate implements Predicate<String, String> {
    private static final long serialVersionUID = 1868230836762136031L;

    @Override
    public boolean apply(Entry<String, String> mapEntry) {
        return (Integer.parseInt(mapEntry.getValue())%2==0);
    }

}
