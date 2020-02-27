package com.whizcontrol.core.model.scheduler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.whizcontrol.core.model.scheduler.LocalDateTimeValue;
import com.whizcontrol.core.model.scheduler.LocalDateValue;
import com.whizcontrol.core.model.scheduler.LocalTimeValue;

public class LocalDateTimeValueTests extends BaseLocalValueTests<LocalDateTimeValue, LocalDateTime> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDateTimeValueTests.class);

    public static LocalDateTimeValue createValidValue() {
        LocalDateTimeValue result = new LocalDateTimeValue();
        result.setDate(LocalDateValueTests.createValidValue());
        result.setTime(LocalTimeValueTests.createValidValue());
        return result;
    }

    public static void populateDataValidationTestValues(Map<String, LocalDateTimeValue> testCases) {
        // test with invalid date
        Map<String, LocalDateValue> dateTests = new HashMap<>();
        LocalDateValueTests.populateDataValidationTestValues(dateTests);
        for (Entry<String, LocalDateValue> entry : dateTests.entrySet()) {
            LocalDateTimeValue value = new LocalDateTimeValue();
            value.setDate(entry.getValue());
            value.setTime(LocalTimeValueTests.createValidValue());
            testCases.put(entry.getKey(), value);
        }

        // test with invalid time
        Map<String, LocalTimeValue> timeTests = new HashMap<>();
        LocalTimeValueTests.populateDataValidationTestValues(timeTests);
        for (Entry<String, LocalTimeValue> entry : timeTests.entrySet()) {
            LocalDateTimeValue value = new LocalDateTimeValue();
            value.setDate(LocalDateValueTests.createValidValue());
            value.setTime(entry.getValue());
            testCases.put(entry.getKey(), value);
        }
    }

    @Override
    protected void getDataValidationTestValues(Map<String, LocalDateTimeValue> testCases) {
        populateDataValidationTestValues(testCases);
    }

    @Override
    protected LocalDateTimeValue getJSONTestValue() {
        return createValidValue();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
