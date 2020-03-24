package com.telecominfraproject.wlan.core.model.equipment;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.json.JsonDeserializationUtils;

public enum ChannelBandwidth {

        auto(0),
        is20MHz(1),
        is40MHz(2),
        is80MHz(3),
        is160MHz(4),
        UNSUPPORTED(-1);

        private final int id;
        private static final Map<Integer, ChannelBandwidth> ELEMENTS = new HashMap<>();

        private ChannelBandwidth(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static ChannelBandwidth getById(int enumId) {
            if (ELEMENTS.isEmpty()) {
                synchronized (ELEMENTS) {
                    if (ELEMENTS.isEmpty()) {
                        //initialize elements map
                        for(ChannelBandwidth met : ChannelBandwidth.values()) {
                            ELEMENTS.put(met.getId(), met);
                        }
                    }
                }
            }
            return ELEMENTS.get(enumId);
        }
        
        /*
         * This will return the numerical bandwidth value
         * 
         * ie: is40mhz will return 40. 
         */
        public int getNumericalValue()
        {
            if(id != 0) // "auto" doesn't have a numerical value
            {
                return 10 * (int) Math.pow(2, id);
            }
            else
            {
                return 0;
            }
        }
        
        @JsonCreator
        public static ChannelBandwidth getByName(String value) {
            return JsonDeserializationUtils.deserializEnum(value, ChannelBandwidth.class, UNSUPPORTED);
        }
        
        public static boolean isUnsupported(ChannelBandwidth value) {
            return UNSUPPORTED.equals(value);
        }
    }
