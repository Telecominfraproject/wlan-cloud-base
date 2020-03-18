package com.telecominfraproject.wlan.core.server.container;

import java.util.Arrays;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author dtoptygin
 *
 */
@Component
@Profile(value = { "use_ssl" })
public class SSLContainerCustomizer extends ServletContainerCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(SSLContainerCustomizer.class);
    
    @Autowired private ApplicationContext applicationContext;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        
        final boolean useX05ClientCertificateAuth = Arrays.asList(applicationContext.getEnvironment().getActiveProfiles()).contains("client_certificate_auth");

        //not used for now, keep code for future reference
//        
//        ServletContextInitializer sci = new ServletContextInitializer() {
//            @Override
//            public void onStartup(ServletContext servletContext) throws ServletException {
//            }
//        };
//        factory.addInitializers(sci);
        
        if(factory instanceof TomcatServletWebServerFactory) {
        	TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
            
            //tomcat factory will create by default only one Http connector - on the port configured in application.properties (server.port)
            //customize that connector to support SSL
            TomcatConnectorCustomizer tomcatConnectorCustomizers = new TomcatConnectorCustomizer() {
                
                @Override
                public void customize(Connector connector) {
                    connector.setAttribute(ConnectorType.CONNECTOR_ATTRIBUTE_NAME, ConnectorType.externalConnector);
                    customizeSslConnector(connector, useX05ClientCertificateAuth, null);

                    enableCompression(connector);

                }
            };
            //this affects only first, default one, connector
            tomcatFactory.addConnectorCustomizers(tomcatConnectorCustomizers );
            //default timeout is 30 minutes, connection is dropped after that by the server
            //tomcatFactory.setSessionTimeout(-1);
            
            disableSessionCookies(tomcatFactory);
        } else {
            LOG.error("Cannot customize embedded container: " + factory);
        }
    }
    

}
