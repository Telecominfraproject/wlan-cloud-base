package com.telecominfraproject.wlan.core.model.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.telecominfraproject.wlan.core.model.pair.PairLongLong;

public class PairLongLongDeserializer extends KeyDeserializer 
{
    @Override
    public PairLongLong deserializeKey(String key, DeserializationContext ctxt) throws IOException, JsonProcessingException 
    {
        return PairLongLong.fromString(key, PairLongLong.class);
    }

}
