package com.telecominfraproject.wlan.core.server.webconfig;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

/**
 * This is an example of ServletPreInvocableHandler. 
 * To enable it - activate spring profile "useExampleServletPreInvocableHandler".
 * 
 * @author dtop
 *
 */
@Component
@Profile("useExampleServletPreInvocableHandler")
public class ExampleServletPreInvocableHandler implements ServletPreInvocableHandler {
    
    final static Logger LOG = LoggerFactory.getLogger(ExampleServletPreInvocableHandler.class);

    @Override
    public void preInvoke(HandlerMethod handlerMethod, Object[] args) {
        
        RequestMapping reqMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
        MethodParameter[] methodParamDefs  = handlerMethod.getMethodParameters();
        Map<String, Object> methodParams = new HashMap<>();
        String pName;
        for(int i=0; i< args.length; i++) {
            if(i< methodParamDefs.length) {
                pName = methodParamDefs[i].getParameterName();
                if(pName==null) {
                    pName = "unknown" + i;
                }
            } else {
                pName = "unknown" + i;
            }
            methodParams.put(pName, args[i]);
        }
        
        LOG.info("Calling doInvoke on {}.{} : {} {} with parameters {} ", handlerMethod.getBean().getClass().getName(), handlerMethod.getMethod().getName(), 
                reqMapping.path(), reqMapping.method(), methodParams);
        
        //TODO: invoke some application logic in here, like fine-grained role-based access control
        if(reqMapping.path()[0].equals("/equipment/forCustomerWithFilter")) {
            throw new RuntimeException("Cannot do that!");
        }


    }

}
