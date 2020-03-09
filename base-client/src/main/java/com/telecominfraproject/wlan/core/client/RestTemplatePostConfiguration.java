package com.telecominfraproject.wlan.core.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

@Configuration
@ConditionalOnClass(RestTemplate.class)
public class RestTemplatePostConfiguration {

    private static final Logger LOG = LoggerFactory
            .getLogger(RestTemplatePostConfiguration.class);

//    @Autowired RestTemplate restTemplate;
//    
//    @PostConstruct
//    private void registerModulesWithObjectMappers() {
//        registerModulesWithObjectMappers(restTemplate);
//    }

    public static void registerModulesWithObjectMappers(AsyncRestTemplate restT) {
        //this is needed so that rest client can produce and consume JSON objects with (and without) _type property 
        for(@SuppressWarnings("rawtypes") HttpMessageConverter c: restT.getMessageConverters()){
            if(c instanceof MappingJackson2HttpMessageConverter){
                LOG.info("Configuring ObjectMapper on AsyncRestTemplate");
                BaseJsonModel.registerAllSubtypes(((MappingJackson2HttpMessageConverter)c).getObjectMapper());
            }
        }
    }

    public static void registerModulesWithObjectMappers(RestTemplate restT) {
        //this is needed so that rest client can produce and consume JSON objects with (and without) _type property 
        for(@SuppressWarnings("rawtypes") HttpMessageConverter c: restT.getMessageConverters()){
            if(c instanceof MappingJackson2HttpMessageConverter){
                LOG.info("Configuring ObjectMapper on RestTemplate");
                BaseJsonModel.registerAllSubtypes(((MappingJackson2HttpMessageConverter)c).getObjectMapper());
            }
        }
    }


}
