package com.telecominfraproject.wlan.core.model.equipment;

/**
 * This has to be implemented by any config that needs to be pushed to a device.
 * 
 * @author erikvilleneuve
 *
 * @param <T>
 */
public interface PushableConfiguration<T> 
{
   public boolean needsToBeUpdatedOnDevice(T previousVersion);
}
