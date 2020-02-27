package com.whizcontrol.core.model.scheduler;

import java.time.LocalTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whizcontrol.core.model.scheduler.LocalTimeValue;

public class LocalTimeValueTests extends BaseLocalValueTests<LocalTimeValue, LocalTime> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTimeValueTests.class);

    public static LocalTimeValue createValidValue() {
        return new LocalTimeValue(LocalTime.of(23, 59));
    }

    public static void populateDataValidationTestValues(Map<String, LocalTimeValue> testCases) {
        LocalTimeValue value = new LocalTimeValue();
        value.setHour(25);
        testCases.put("invalid hour", value.clone());

        value.setHour(0);
        value.setMinute(60);
        testCases.put("invalid minute", value.clone());
    }

    @Override
    protected void getDataValidationTestValues(Map<String, LocalTimeValue> testCases) {
        populateDataValidationTestValues(testCases);
    }

    @Override
    protected LocalTimeValue getJSONTestValue() {
        return createValidValue();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
