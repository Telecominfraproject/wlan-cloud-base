package com.whizcontrol.core.model.json;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;


/**
 * @author dtop
 * This class currently does nothing, but let's keep it around for a couple of months 
 * - in case we need some of the commented out functionality. 
 */
@Configuration
//@ConditionalOnClass(ObjectMapper.class)
public class JsonConfiguration implements BeanPostProcessor {

//        @Autowired
//        private ListableBeanFactory beanFactory;
//        @Autowired
//        private MappingJackson2HttpMessageConverter converter;
//
//        @PostConstruct
//        private void registerModulesWithObjectMappers() {
//            for (ObjectMapper objectMapper : getBeans(ObjectMapper.class)) {
//                BaseJsonModel.registerAllSubtypes(objectMapper);
//            }
//            
//            BaseJsonModel.registerAllSubtypes(converter.getObjectMapper());
//        }
//
//        private <T> Collection<T> getBeans(Class<T> type) {
//            return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type)
//                    .values();
//        }
        
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            return bean;
        }

        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//            if (bean instanceof MappingJackson2HttpMessageConverter) {
//                System.err.println("*** Configuring MappingJackson2HttpMessageConverter "+beanName);
//                MappingJackson2HttpMessageConverter jsonConverter =
//                        (MappingJackson2HttpMessageConverter) bean;
//                ObjectMapper objectMapper = jsonConverter.getObjectMapper();
//                BaseJsonModel.registerAllSubtypes(objectMapper);
////                objectMapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE , "_type");
////                TypeResolverBuilder<?> typer = ;
////                objectMapper.setDefaultTyping(typer);
//
//                jsonConverter.setObjectMapper(objectMapper);
//            } else if(bean instanceof ObjectMapper){
//                System.err.println("*** Configuring ObjectMapper "+beanName);
////                ((ObjectMapper) bean).enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE , "_type");
//                BaseJsonModel.registerAllSubtypes((ObjectMapper) bean);
//            }
            return bean;
        }
}
