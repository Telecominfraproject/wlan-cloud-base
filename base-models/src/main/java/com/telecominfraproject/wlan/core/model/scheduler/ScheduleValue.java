/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.time.temporal.Temporal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;

/**
 * Value class use to make Temporal value serializable using JSON.
 * 
 * @param T
 *            - Temporal class
 * @author yongli
 *
 */
public interface ScheduleValue<T extends Temporal> {
    /**
     * Validate the value.
     * 
     * @throws DsDataValidationException
     */
    public void validateValue();

    /**
     * Holds the value
     * 
     * @return value
     */
    @JsonIgnore
    public T getValue();

    /**
     * Force a JSon encoder
     * 
     * @return
     */
    public String toPrettyString();
}
