/**
 * 
 */
package com.telecominfraproject.wlan.core.model.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

/**
 * @author yongli
 *
 */
public class JsonDeserializationUtils {
    private static final Logger LOG = LoggerFactory.getLogger(JsonDeserializationUtils.class);

    private JsonDeserializationUtils() {

    }

    /**
     * Deserialize enumeration with default value when it's unknown.
     * 
     * @param jsonValue
     * @param enumType 
     * @param defaultValue
     * @return decoded value
     */
    public static <E extends Enum<E>> E deserializEnum(String jsonValue, Class<E> enumType, E defaultValue) {
        if (null == jsonValue) {
            return null;
        }
        try {
            return E.valueOf(enumType, jsonValue);
        } catch (IllegalArgumentException e) {
            LOG.trace("Failed to parse {} value {}", enumType.getSimpleName(), jsonValue, e);
            return defaultValue;
        }
    }
}
