package com.telecominfraproject.wlan.core.model.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * 
 * Will quicky and simply create/decode tokens for the password change request.
 * 
 * @author erikvilleneuve
 *
 */
public class TokenUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TokenUtils.class);

    public static final String PASSWORD = "(!This is the password!)";

    private static String CIPHER = "AES";

    /**
     * Will return null if the key's invalid.
     * 
     * @param str
     * @param clazz
     * @param password
     * @param encoder
     *            optional, default is
     *            {@link TokenEncoder#Base64UrlTokenEncoder}
     * @return
     */
    public static <T extends BaseJsonModel> T decodeToken(String str, Class<T> clazz, String password,
            TokenEncoder encoder) {
        String decrypted = decrypt(str, password, encoder);

        if (decrypted != null) {
            return BaseJsonModel.fromString(decrypted, clazz);
        }

        return null;
    }

    /**
     * 
     * @param encryptedPayloadStr
     * @param key
     * @param encoder
     *            optional, default is {@link #Base64UrlTokenEncoder}
     * @return
     */
    public static String decrypt(String encryptedPayloadStr, String key, TokenEncoder encoder) {
        try {
            byte[] encrytedPayload = decodeFromString(encoder, encryptedPayloadStr);

            Cipher c = Cipher.getInstance(CIPHER);
            SecretKeySpec k = new SecretKeySpec(pad(key, 16).getBytes(), CIPHER);
            c.init(Cipher.DECRYPT_MODE, k);

            byte[] decrypted = c.doFinal(encrytedPayload);

            return new String(decrypted);
        } catch (Exception e) {
            LOG.warn("Issues decrypting: ", e);
            return null;
        }
    }

    /**
     * Will encode a token given a password (any length, we pad it at 16 bytes).
     * 
     * @param encoder
     *            optional, default is
     *            {@link TokenEncoder#Base64UrlTokenEncoder}
     * 
     */
    public static <T extends BaseJsonModel> String encodeToken(T obj, String password, TokenEncoder encoder) {
        if (obj != null) {
            // We strip the whitespaces;
            String serializedObject = obj.toString().replaceAll("\\s+", "");
            return encrypt(serializedObject, password, encoder);
        }

        return null;
    }

    /**
     * 
     * @param payload
     * @param key
     * @param encoder
     *            optional, default is
     *            {@link TokenEncoder#Base64UrlTokenEncoder}
     * @return
     */
    public static String encrypt(String payload, String key, TokenEncoder encoder) {
        try {
            Cipher c = Cipher.getInstance(CIPHER);
            SecretKeySpec k = new SecretKeySpec(pad(key, 16).getBytes(), CIPHER);
            c.init(Cipher.ENCRYPT_MODE, k);

            byte[] encrypted = c.doFinal(payload.getBytes());

            return encodeToString(encoder, encrypted);
        } catch (Exception e) {
            LOG.warn("Issues encrypting: ", e);
            return null;
        }
    }

    private static byte[] decodeFromString(TokenEncoder encoder, String value) {
        if (encoder != null) {
            return encoder.decodeFromString(value);
        }
        return TokenEncoder.Base64UrlTokenEncoder.decodeFromString(value);
    }

    private static String encodeToString(TokenEncoder encoder, byte[] encrypted) {
        if (null != encoder) {
            return encoder.encodeToString(encrypted);
        }
        return TokenEncoder.Base64UrlTokenEncoder.encodeToString(encrypted);
    }

    private static String generatePad(int numCharToGenerate) {
        StringBuilder sb = new StringBuilder(numCharToGenerate);

        for (int i = 0; i < numCharToGenerate; i++) {
            sb.append('x');
        }

        return sb.toString();
    }

    /**
     * If the key is too long or too short, this method will either truncate to
     * 16 bytes or pad the key to 16 bytes.
     * 
     * @param key
     * @param key
     *            length
     * @return
     */
    private static String pad(String key, int keyLength) {
        if (key == null) {
            throw new IllegalArgumentException("Invalid null key");
        }

        if (key.length() > keyLength) {
            return key.substring(0, keyLength);
        } else if (key.length() < keyLength) {
            return key + generatePad(keyLength - key.length());
        } else {
            return key;
        }
    }

}
