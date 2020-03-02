package com.telecominfraproject.wlan.core.model.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * This class is used to find property values when running outside of Spring environment
 * @author dtop
 *
 */
public class SystemAndEnvPropertyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SystemAndEnvPropertyResolver.class);

    public static String getPropertyAsString(String propertyName, String defaultValue) {
        // We will simulate spring PropertySourcesPropertyResolver
        // try systemProperties
        String ret = System.getProperty(propertyName);
        if (ret == null) {
            // try systemEnvironment
            String envPropertyName = propertyName.replace('.', '_');
            ret = System.getenv(envPropertyName);
            // also try upper case
            if (ret == null) {
                ret = System.getenv(envPropertyName.toUpperCase());
            }
        }
        if (ret == null) {
            ret = defaultValue;
        }
        if (ret != null) {
            ret = ret.trim();
        }
        return ret;
    }
    
    /**
     * Iterate through all instance variables, and for all that are String or int and that are null or 0 - 
     * find @Value annotation and use it with SystemAndEnvPropertyResolver.
     * 
     * @return supplied object initialized from System Properties or Environment Variables
     */
    public static <T> T initOutsideOfSpringApp(T ret){
        
        Class<?> clazz = ret.getClass();
        String valueAnnotationPrefix = "@org.springframework.beans.factory.annotation.Value(value=${";
        for(Field f: clazz.getDeclaredFields()){
            f.setAccessible(true);
            try {
                //we'll initialize only String and int fields
                if((f.getType().equals(String.class) && f.get(ret) == null) || (f.getType().equals(int.class) && ((int)f.get(ret)) == 0)){
                    for(Annotation annotation: f.getDeclaredAnnotations()){
                        String aVal = annotation.toString();
                        //example of aVal:
                        // @org.springframework.beans.factory.annotation.Value(value=${whizcontrol.hazelcast.systemEventsRecordIndex.ttlSeconds:7200})
                        if(aVal.startsWith(valueAnnotationPrefix)){
                            int indexOfSeparator = aVal.lastIndexOf(':');
                            String propName = aVal.substring(valueAnnotationPrefix.length(), indexOfSeparator);
                            String propValue = aVal.substring(indexOfSeparator + 1, aVal.length() - 2);
                            
                            propValue = SystemAndEnvPropertyResolver.getPropertyAsString(propName, propValue);
                            
                            LOG.debug("Initializing {} field {} from property {} with value ({})", clazz.getSimpleName(), f.getName(), propName, propValue );
                            
                            if(f.getType().equals(String.class)){
                                f.set(ret, propValue);
                            } else if(f.getType().equals(int.class)){
                                f.set(ret, Integer.parseInt(propValue));
                            } else {
                               throw new ConfigurationException("Cannot initialize " + clazz.getSimpleName() + " field " + f.getName());
                            }
                                
                            break;
                        }
                        LOG.debug("Skipping {} field {} annotated with {} ", clazz.getSimpleName(), f.getName(), aVal);
                    }
                } else {
                    LOG.debug("{} Field {} ({}) has no annotations", clazz.getSimpleName(), f.getName(), f.getType());
                }
                    
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new ConfigurationException("Cannot initialize " + 
                        clazz.getSimpleName() + " field " + f.getName(), e);
            }
        }
        
        return ret;
    }    
    
}
