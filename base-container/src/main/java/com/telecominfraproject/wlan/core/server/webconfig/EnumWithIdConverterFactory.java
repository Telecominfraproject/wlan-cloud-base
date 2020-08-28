package com.telecominfraproject.wlan.core.server.webconfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;

public class EnumWithIdConverterFactory implements ConverterFactory<String, EnumWithId> {
    
    private static Map<String, Converter<String,  ? extends EnumWithId>> converters = new ConcurrentHashMap<>();
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends EnumWithId> Converter<String, T> getConverter(Class<T> targetType) {
        
        Converter<String, ? extends EnumWithId> ret = converters.get(targetType.getSimpleName());
        if(ret == null) {
            try {
                Method m = targetType.getDeclaredMethod("getByName", String.class);
                
                ret = new Converter<String, T>() {
                    @Override
                    public T convert(String source) {
                        try {
                            return (T) m.invoke(null, source);
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalStateException(e);
                        } catch (InvocationTargetException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                };
                
                converters.put(targetType.getSimpleName(), ret);
                
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(""+ targetType.getName()+" has to define static getByName method", e);
            } catch (SecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        
        return (Converter<String, T>) ret;
    }

}
