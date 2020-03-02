/**
 * 
 */
package com.telecominfraproject.wlan.core.server.async;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * @author yongli
 *
 */
public class UncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler#
     * handleUncaughtException(java.lang.Throwable, java.lang.reflect.Method,
     * java.lang.Object[])
     */
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        LOG.error("Uncaught Exception in method: " + method.getDeclaringClass() + '.' + method.getName(), ex);
    }

}
