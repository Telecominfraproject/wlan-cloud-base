package com.telecominfraproject.wlan.core.model.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JacksonJsonParser;

import com.telecominfraproject.wlan.server.exceptions.SerializationException;

public class JWTUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JWTUtil.class);
    
    public static final String JWT_CLIENT_ID_KEY = "azp";
    
    private JWTUtil() {
    }

    public static Map<String, Object> getJWTBodyAsJSON(String encoded) {
        try {
            String[] split = encoded.split("\\.");
            if (split == null || split.length != 3) {
                throw new SerializationException("Passed in string is not a JWT: " + encoded);
            }
            
            Map<String, Object> jsonMap = getJson(split[1]);
            LOG.debug("Decoded Body: {}", jsonMap);
            return jsonMap;
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException("Error parsing JWT", e);
        }
    }
    
    private static Map<String, Object> getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.getDecoder().decode(strEncoded);
        String result = new String(decodedBytes, StandardCharsets.UTF_8);
        JacksonJsonParser parser = new JacksonJsonParser();
        Map<String, Object> jsonMap = parser.parseMap(result);
        
        return jsonMap;
    }
}
