/**
 * 
 */
package com.telecominfraproject.wlan.core.model.scheduler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.reflections.Reflections;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.datastore.exceptions.DsDataValidationException;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * Setting used by scheduler.
 * 
 * All implementation must implements a constructor takes
 * {@linkplain java.time.Duration}
 * 
 * @author yongli
 *
 */
public abstract class ScheduleSetting extends BaseJsonModel {
    private static final long serialVersionUID = 8467140690884044897L;

    /**
     * Get the list of time zone supported.
     * 
     * @return set
     */
    public static SortedSet<String> allZoneIds() {
        return new TreeSet<>(ZoneId.getAvailableZoneIds());
    }

    private transient ZoneId localZoneId;

    private String timezone;

    public ScheduleSetting(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public ScheduleSetting clone() {
        return (ScheduleSetting) super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ScheduleSetting)) {
            return false;
        }
        ScheduleSetting other = (ScheduleSetting) obj;
        if (this.timezone == null) {
            if (other.timezone != null) {
                return false;
            }
        } else if (!this.timezone.equals(other.timezone)) {
            return false;
        }
        return true;
    }

    /**
     * Get the time zone setting for the schedule
     * 
     * @return zone or default
     */
    @JsonIgnore
    public ZoneId getLocalZoneId() {
        if (null == localZoneId) {
            if (null != this.getTimezone()) {
                this.localZoneId = ZoneId.of(this.getTimezone(), ZoneId.SHORT_IDS);
            } else {
                this.localZoneId = ZoneId.systemDefault();
            }
        }
        return this.localZoneId;
    }

    /**
     * Provide the time window for the schedule
     * 
     * @param currentTime
     *            - epoch time
     * @return null if non available
     */
    public abstract EpochTimeWindow getNextWindows(long currentTime);

    public String getTimezone() {
        return timezone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.timezone == null) ? 0 : this.timezone.hashCode());
        return result;
    }

    /**
     * Test if schedule contains any interval
     */
    @JsonIgnore
    public boolean isEmpty() {
        return false;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.localZoneId = null;
    }

    /**
     * Validate the setting.
     * 
     * @param minimumDuration
     *            - optional check for minimum duration
     * 
     * @throws DsDataValidationException
     */
    public void validateSetting(Duration minimumDuration) {
        try {
            getLocalZoneId();
        } catch (DateTimeException exp) {
            throw new DsDataValidationException("invalid timezone setting", exp);
        }
    }

    protected abstract Logger getLogger();

    /**
     * Use for testing purpose only. Create a set of all type of schedule
     * setting which will pass {@link ScheduleSetting#validateSetting(Duration)}
     * 
     * @param duration
     * 
     * @return set of instance
     */
    public static List<ScheduleSetting> createTestSettings(Duration duration) {
        List<ScheduleSetting> result = new LinkedList<>();

        Reflections reflections = new Reflections(ScheduleSetting.class.getPackage().getName());
        Set<Class<? extends ScheduleSetting>> scheduleClass = reflections.getSubTypesOf(ScheduleSetting.class);

        try {
            for (Class<? extends ScheduleSetting> clazz : scheduleClass) {
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                Constructor<? extends ScheduleSetting> constructor = clazz
                        .getDeclaredConstructor(java.time.Duration.class);
                result.add(constructor.newInstance(duration));
            }
            return result;
        } catch (Exception exp) {
            throw new GenericErrorException("Failed to create test settings", exp);
        }
    }
}
