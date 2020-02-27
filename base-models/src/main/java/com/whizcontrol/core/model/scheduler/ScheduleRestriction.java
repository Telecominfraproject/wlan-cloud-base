/**
 * 
 */
package com.whizcontrol.core.model.scheduler;

import java.time.Duration;

import com.whizcontrol.datastore.exceptions.DsDataValidationException;

/**
 * Schedule restriction defined for the system.
 * 
 * @author yongli
 *
 */
public enum ScheduleRestriction {
    /**
     * Firmware Maintenance window restriction. Minimum Duration 1 hour.
     */
    FIRMWARE_MAINTENANCE(Duration.ofHours(1));

    private final Duration minimumDuration;

    ScheduleRestriction(final Duration minimumDuration) {
        this.minimumDuration = minimumDuration;
    }

    public Duration getMinimumDuration() {
        return minimumDuration;
    }

    /**
     * Check if schedule meets the restriction
     * 
     * @param firmwareUpgradeSchedule
     * 
     * @throws DsDataValidationException
     *             if schedule is not valid
     */
    public void isValidSchedule(ScheduleSetting firmwareUpgradeSchedule) {
        if (null == firmwareUpgradeSchedule) {
            return;
        }
        firmwareUpgradeSchedule.validateSetting(this.minimumDuration);
    }
}
