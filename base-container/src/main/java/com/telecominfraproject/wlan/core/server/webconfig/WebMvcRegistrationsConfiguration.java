/**
 * 
 */
package com.telecominfraproject.wlan.core.server.webconfig;

import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

/**
 * @author yongli
 * @author dtop
 *
 */
@Configuration
public class WebMvcRegistrationsConfiguration {
    final static Logger LOG = LoggerFactory.getLogger(WebMvcRegistrationsConfiguration.class);

    @Autowired(required = false)
    private List<ServletPreInvocableHandler> servletPreInvocableHandlers;
    
    /**
     * Use adaptor to filter out RequestMapping methods based on condition.<br>
     * Provide a way to register pre-invoke servlet handlers - to have common logic applied 
     * after the parameters have been parsed and converted to java objects but before the Controller method itself is called.
     * @see ExampleServletPreInvocableHandler
     * @see ServletPreInvocableHandler
     * 
     * @param environment
     * @return
     */
    @Bean
    public WebMvcRegistrations mvcRegistrations(Environment environment) {
        LOG.info("Customizing WebMvcRegistrations");
        
        if(servletPreInvocableHandlers!=null) {
            servletPreInvocableHandlers.forEach(h -> LOG.info("registering pre-invoke servlet handler {}", h) );
        }

        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new RequestMappingHandlerMapping() {
                    @Override
                    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
                        /*-
                        if (null != AnnotatedElementUtils.findMergedAnnotation(method, Deprecated.class)) {
                            LOG.info("Skipping deprecated method {}", method.getName());
                            return null;
                        }
                        */
                        RequestMappingInfo result = super.getMappingForMethod(method, handlerType);
                        if (null != result) {
                            Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);
                            if(null != profile) {
	                            Profiles profiles =Profiles.of(profile.value());
	                            if (!environment.acceptsProfiles(profiles)) {
	                                LOG.info("Skipped Mapping {} based on profile condition {}", result, profile.value());
	                                return null;
	                            }
                            }
                            
                            LOG.info("Mapped endpoint {} -> {} -> {} {}({}) ", result, handlerType.getSimpleName(), method.getAnnotatedReturnType(), method.getName(), method.getAnnotatedParameterTypes());
                        }
                        return result;
                    }
                };
            }
            
            @Override
            public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
                return  new RequestMappingHandlerAdapter() {
                    @Override
                    protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
                        return new ServletInvocableHandlerMethod(handlerMethod) {
                            @Override
                            protected Object doInvoke(Object... args) throws Exception {
                                
                                //apply all registered pre-invoke servlet handlers
                                if(servletPreInvocableHandlers!=null) {
                                    servletPreInvocableHandlers.forEach(h -> h.preInvoke(handlerMethod, args) );
                                }
                                
                                return super.doInvoke(args);
                            }
                        };
                    }
                };                
            }
            
        };
    }
}
