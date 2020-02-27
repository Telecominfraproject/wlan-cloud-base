package com.whizcontrol.core.server.controller.util;

import com.whizcontrol.datastore.exceptions.DsConcurrentModificationException;

/**
 * Since some of our APIs involve switching states, I prefer to have
 * the retry logic outside of the main code path.
 * 
 * @author erikvilleneuve
 *
 */
public class DBRetryUtil
{
    public static interface DBAccessorInterface<T>
    {
        public T getObject();
        public T updateObject(T object);
        public T applyChange(T object);
    }

    public static <T> T updateObject(DBAccessorInterface<T> handler)
    {
        int maxRetries = 10;
        DsConcurrentModificationException lastException;
        
        do
        {
            // We get the latest
            T object = handler.getObject();
            
            try
            {
                return handler.updateObject(handler.applyChange(object));
            }
            catch(DsConcurrentModificationException e)
            {
                // let's retry!
                maxRetries--;
                lastException = e;
            }
            
            
        } while(maxRetries > 0);
        
        throw lastException;
        
    }
}
