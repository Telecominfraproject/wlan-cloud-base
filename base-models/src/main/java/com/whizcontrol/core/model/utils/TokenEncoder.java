/**
 * 
 */
package com.whizcontrol.core.model.utils;

import java.util.Base64;

import io.seruco.encoding.base62.Base62;

/**
 * @author yongli
 *
 */
public interface TokenEncoder {

    String encodeToString(byte[] value);

    byte[] decodeFromString(String value);

    public static final TokenEncoder Base64UrlTokenEncoder = new TokenEncoder() {

        @Override
        public String encodeToString(byte[] encrypted) {
            return Base64.getUrlEncoder().encodeToString(encrypted);
        }

        @Override
        public byte[] decodeFromString(String value) {
            return Base64.getUrlDecoder().decode(value);
        }
    };

    /**
     * @see https://github.com/seruco/base62
     */
    public static final TokenEncoder Base62TokenEncoder = new TokenEncoder() {
        final Base62 base62 = Base62.createInstance();

        @Override
        public String encodeToString(byte[] value) {
            return new String(base62.encode(value));
        }

        @Override
        public byte[] decodeFromString(String value) {
            return base62.decode(value.getBytes());
        }
    };
}
