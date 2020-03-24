package com.telecominfraproject.wlan.core.server.webconfig;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtoptygin
 *
 */
@Configuration
//@EnableWebMvc - DTOP: do not use this, it will break mapping for index.html file
// see http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-auto-configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired private Environment environment;
    private static final Logger LOG = LoggerFactory.getLogger(WebConfig.class);
    private static final String WEB_RESOURCE_PROP = "tip.wlan.webResources";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        List<String> resourceLocations = new ArrayList<>();
        resourceLocations.add("classpath:/static/");

        String resourceLocation = environment.getProperty(WEB_RESOURCE_PROP);
        if(resourceLocation!=null){
            resourceLocations.add(resourceLocation);
            LOG.info("This server will provide access to files under {}", resourceLocation);
        }
        ResourceHandlerRegistration resourceHandlerRegistration = registry
            .addResourceHandler("/**")
            .addResourceLocations(resourceLocations.toArray(new String[0]));

        if(environment.getProperty("tip.wlan.webResources.cachePeriodSec")!=null){
            //0 means no caching
            //absense of the call below means relying on file last-modified timestamps
            resourceHandlerRegistration.setCachePeriod(Integer.getInteger("tip.wlan.webResources.cachePeriodSec", 0));
        }


        super.addResourceHandlers(registry);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer matcher) {
        //http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated

        //without this any request that has a @PathVariable annotation /{email} and ends with /user@example.org will be interpreted as email=user@example (truncate at the last dot)
        matcher.setUseRegisteredSuffixPatternMatch(true);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //this is needed so that servlets can consume and produce JSON objects with (and without) _type parameters
        LOG.info("extending MessageConverters to understand BaseJsonModel");
        for(HttpMessageConverter<?> c: converters){
            if(c instanceof MappingJackson2HttpMessageConverter){
                BaseJsonModel.registerAllSubtypes(((MappingJackson2HttpMessageConverter)c).getObjectMapper());
            }
        }
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // This is needed so that @RequestParam annotations in the servlet
        // methods can be used with BaseJsonModel and its descendants and its
        // collections.

        //Use GenericlConverter here, simple one does not work
        LOG.info("Adding custom converters to process BaseJsonModel");

        registry.addConverter(new WebGenericConverter());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(byteArrayHttpMessageConverter());
    }

    /**
     * Create byte array http message converter
     * @return
     */
    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        arrayHttpMessageConverter.setSupportedMediaTypes(getSupportedMediaTypes());
        return arrayHttpMessageConverter;
    }

    private List<MediaType> getSupportedMediaTypes() {
        List<MediaType> list = new ArrayList<>();
        list.add(MediaType.APPLICATION_OCTET_STREAM);
        list.add(MediaType.IMAGE_JPEG);
        list.add(MediaType.IMAGE_PNG);
        return list;
    }

}
