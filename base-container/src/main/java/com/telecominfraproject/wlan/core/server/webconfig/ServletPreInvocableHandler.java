package com.telecominfraproject.wlan.core.server.webconfig;

import org.springframework.web.method.HandlerMethod;

/**
 * This interface defines a method that is called after all the parameters are deserialized and converted to java objects but before the Controller method gets called.
 * Can be used for fine-grained role-based access control.
 * Many classes can implement this interface. 
 * When marked as @Bean, all such classes will be registered and called before every Controller method.
 * 
 * @see ExampleServletPreInvocableHandler
 * @author dtop
 *
 */
public interface ServletPreInvocableHandler {
    void preInvoke(HandlerMethod handlerMethod, Object[] args);
}
