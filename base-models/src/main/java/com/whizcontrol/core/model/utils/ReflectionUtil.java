package com.whizcontrol.core.model.utils;

import java.lang.reflect.Field;

public class ReflectionUtil 
{
    /**
     * A simple reflection utility that will tells us if an object contains any of the fieldnames specified.
     * 
     * It will return true if the objects contains ANY ONE of the specified fieldName
     * 
     * 
     * @param obj
     * @param fieldName
     * @return
     */
    public static boolean containsEitherFields(Object obj, String ... fieldNames)
    {
        Class<?> clazz = obj.getClass();
            
        while(clazz != null)
        {
            for(String fieldName : fieldNames)
            {
                try
                {
                    Field field =  clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return true;
                }
                catch(NoSuchFieldException e)
                {
                    // nothing
                }
            }
            
            clazz = clazz.getSuperclass();
        }

        return false;
    }

}
