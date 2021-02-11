package com.telecominfraproject.wlan.core.model.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Characters plus and slash, that would need quoting in URLs, are replaced with hyphen and underscore, respectively.
 * @see com.fasterxml.jackson.core.Base64Variants.MODIFIED_FOR_URL
 */
public class Base64UrlByteArrayDeserializer extends JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        
        JsonToken token = p.currentToken();
        
        // we only support base64 encoded String
        if (token == JsonToken.VALUE_STRING) {
            try {
                return p.getBinaryValue(Base64Variants.MODIFIED_FOR_URL);
            } catch (JsonParseException e) {
                String msg = e.getOriginalMessage();
                if (msg.contains("base64")) {
                    return (byte[]) ctxt.handleWeirdStringValue(byte[].class,
                            p.getText().replace(' ', '+'), msg);
                }
            }
        }
        
        throw new JsonParseException(p, "Cannot convert to byte[]");
    }
}
