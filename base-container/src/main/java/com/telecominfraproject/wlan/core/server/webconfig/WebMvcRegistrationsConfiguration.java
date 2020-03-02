/**
 * 
 */
package com.telecominfraproject.wlan.core.server.webconfig;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebMvcRegistrations;
import org.springframework.boot.autoconfigure.web.WebMvcRegistrationsAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
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
     * Use adaptor to filter out RequestMapping method based on condition
     * 
     * @param enviornment
     * @return
     */
    @Bean
    public WebMvcRegistrations mvcRegistrations(Environment enviornment) {
        LOG.info("Activating WebMvcRegistrationsAdapter");
        return new WebMvcRegistrationsAdapter() {
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
                        LOG.trace("getMappingForMethod({}, {})", method.getName(), handlerType.getSimpleName());
                        RequestMappingInfo result = super.getMappingForMethod(method, handlerType);
                        if (null != result) {
                            Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);
                            if (null != profile && !enviornment.acceptsProfiles(profile.value())) {
                                LOG.info("Skipped Mapping {} based on profile condition {}", result, profile.value());
                                return null;
                            }
                        }
                        return result;
                    }
                };
            }
        };
    }
}
