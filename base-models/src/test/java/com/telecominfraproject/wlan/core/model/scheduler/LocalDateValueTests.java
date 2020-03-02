package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.LocalDate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.scheduler.LocalDateValue;

public class LocalDateValueTests extends BaseLocalValueTests<LocalDateValue, LocalDate> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDateValueTests.class);

    public static LocalDateValue createValidValue() {
        return new LocalDateValue(LocalDate.of(2016, 2, 29));
    }

    public static void populateDataValidationTestValues(Map<String, LocalDateValue> invalidDataTests) {
        LocalDateValue localDate = new LocalDateValue();
        localDate.setMonth(2);
        localDate.setYear(2015);
        localDate.setDay(29);
        invalidDataTests.put("invalid leap day", localDate.clone());

        localDate.setMonth(11);
        localDate.setDay(31);
        invalidDataTests.put("invalid day", localDate.clone());

        localDate.setMonth(13);
        invalidDataTests.put("invalid month", localDate.clone());
    }

    @Override
    protected void getDataValidationTestValues(Map<String, LocalDateValue> invalidDataTests) {
        populateDataValidationTestValues(invalidDataTests);
    }

    @Override
    protected LocalDateValue getJSONTestValue() {
        return createValidValue();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
