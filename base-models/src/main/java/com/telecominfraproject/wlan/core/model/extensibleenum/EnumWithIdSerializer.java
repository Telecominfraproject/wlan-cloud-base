package com.telecominfraproject.wlan.core.model.extensibleenum;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * <br>This class serializes descendants of EnumWithId classes as simple strings using getName() method.
 * <br>
 * @author dtop
 *
 */
public class EnumWithIdSerializer extends JsonSerializer<EnumWithId>{

    @Override
    public void serialize(EnumWithId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getName());        
    }

}
