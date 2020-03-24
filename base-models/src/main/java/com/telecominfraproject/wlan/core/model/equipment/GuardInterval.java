package com.telecominfraproject.wlan.core.model.equipment;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

/**
 * 802.11 guard interval[edit] The standard symbol guard interval used in 802.11
 * OFDM is 0.8 μs. To increase data rate, 802.11n added optional support for a
 * 0.4 μs guard interval. This provides an 11% increase in data rate.
 * 
 * The shorter guard interval results in a higher packet error rate when the
 * delay spread of the channel exceed the guard interval and/or if timing
 * synchronization between the transmitter and receiver is not precise. A scheme
 * could be developed to work out whether a short guard interval would be of
 * benefit to a particular link. To reduce complexity, manufacturers typically
 * only implement a short guard interval as a final rate adaptation step when
 * the device is running at its highest data rate.
 * 
 * @author yongli
 *
 */
public enum GuardInterval {
    /**
     * Long Guard Interval
     */
    LGI(1),
    /**
     * Short Guard Interval
     */
    SGI(2),
    
    UNSUPPORTED(-1);

    private final int id;
    private static final Map<Integer, GuardInterval> ELEMENTS = new HashMap<>();

    private GuardInterval(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static GuardInterval getById(int enumId) {
        if (ELEMENTS.isEmpty()) {
            synchronized (ELEMENTS) {
                if (ELEMENTS.isEmpty()) {
                    // initialize elements map
                    for (GuardInterval met : GuardInterval.values()) {
                        ELEMENTS.put(met.getId(), met);
                    }
                }
            }
        }
        return ELEMENTS.get(enumId);
    }

    @JsonCreator
    public static GuardInterval getByName(String value) {
        return JsonDeserializationUtils.deserializEnum(value, GuardInterval.class, UNSUPPORTED);
    }

    public static boolean isUnsupported(GuardInterval value) {
        return UNSUPPORTED.equals(value);
    }
}
