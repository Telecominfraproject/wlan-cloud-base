package com.telecominfraproject.wlan.core.model.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Characters plus and slash, that would need quoting in URLs, are replaced with hyphen and underscore, respectively.
 * @see com.fasterxml.jackson.core.Base64Variants.MODIFIED_FOR_URL
 */
public class Base64UrlByteArraySerializer extends JsonSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(Base64Variants.MODIFIED_FOR_URL.encode(value));
    }

}
