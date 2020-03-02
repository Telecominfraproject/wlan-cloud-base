package com.telecominfraproject.wlan.core.model.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;

import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

public class CloneUtil 
{
    /**
     * This method will create a child from a parent instance (without the child fields 
     * populated).
     * 
     * @param parent
     * @param childClass
     * @return
     */
    public static <X, Y extends X> Y createBlankChildFromParent(X parent, Class<Y> childClass) {
        // We create an instance of the child class
        try {
            Constructor<Y> constructor = childClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            Y child = constructor.newInstance();

            BeanUtils.copyProperties(parent, child);
            return child;
        } catch (Exception e) {
            throw new GenericErrorException("Failed to create blank object of " + childClass.getSimpleName(), e);
        }
    }

    
    /**
     * 
     * This will return a list of the parent type of the children type.
     * 
     * ie: List<Vehicle> vehicles = createListOfParentType(cars);
     * 
     * @param children
     * @return
     */
    public static <X, Y extends X> List<X> createListOfParentType(List<Y> children)
    {
        if(children != null)
        {
            List<X> returnValue = new ArrayList<>(children.size());

            for(Y child : children)
            {
                returnValue.add( (X) child);
            }
            
            return returnValue;
        }
        
        return Collections.emptyList();
        
    }
    
}
 