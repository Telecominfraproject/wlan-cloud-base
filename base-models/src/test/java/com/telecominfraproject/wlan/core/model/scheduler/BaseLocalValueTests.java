/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.slf4j.Logger;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.scheduler.ScheduleValue;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;

/**
 * @author yongli
 *
 */
public abstract class BaseLocalValueTests<V extends ScheduleValue<T>, T extends Temporal> {
    @Test
    public void testDataValidation() {
        Map<String, V> invalidDateTests = new HashMap<>();
        getDataValidationTestValues(invalidDateTests);
        for (Entry<String, V> testEntry : invalidDateTests.entrySet()) {
            testInvalidValue(testEntry.getKey(), testEntry.getValue());
        }
    }

    @Test
    public void testJSONSerialization() {
        V testValue = getJSONTestValue();
        testValue.validateValue();
        verifyJSONSerialization(testValue);
    }

    private void testInvalidValue(final String test, final V value) {
        try {
            value.validateValue();
            fail("Failed " + test + ": " + value);
        } catch (DsDataValidationException exp) {
            getLogger().debug("Passed Test {}: {}", test, exp.getLocalizedMessage());
        }
    }

    private void verifyJSONSerialization(final V localDate) {
        String jsonString = localDate.toPrettyString();
        getLogger().debug("Test {}", jsonString);
        BaseJsonModel result = BaseJsonModel.fromString(jsonString, BaseJsonModel.class);
        assertEquals(localDate, result);
    }

    /**
     * Generate a set of test case for invalid date
     */
    abstract protected void getDataValidationTestValues(Map<String, V> testCases);

    /**
     * generate a value for JSON serialization test
     * 
     * @return
     */
    abstract protected V getJSONTestValue();

    abstract protected Logger getLogger();
}
