/**
 * 
 */
package com.telecominfraproject.wlan.core.server.webconfig;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * @author yongli
 *
 */
@Configuration
public class WebMvcRegistrationsConfiguration {
    final static Logger LOG = LoggerFactory.getLogger(WebMvcRegistrationsConfiguration.class);

    /**
     * Use adaptor to filter out RequestMapping methods based on condition
     * 
     * @param environment
     * @return
     */
    @Bean
    public WebMvcRegistrations mvcRegistrations(Environment environment) {
        LOG.info("Customizing WebMvcRegistrations");
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
        };
    }
}
