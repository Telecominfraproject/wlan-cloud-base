package com.telecominfraproject.wlan.core.server.container;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author dtoptygin
 *
 */
@Component
@Profile(value = { "no_ssl" })
public class NoSSLContainerCustomizer extends ServletContainerCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(NoSSLContainerCustomizer.class);

    @Autowired private ApplicationContext appContext;
    
    @Override
    public void customize(ConfigurableEmbeddedServletContainer factory) {
                
        if(factory instanceof TomcatEmbeddedServletContainerFactory) {
            TomcatEmbeddedServletContainerFactory tomcatFactory = (TomcatEmbeddedServletContainerFactory) factory;
            
            //tomcat factory will create by default only one Http connector - on the port configured in application.properties (server.port)
            TomcatConnectorCustomizer tomcatConnectorCustomizers = new TomcatConnectorCustomizer() {
                
                @Override
                public void customize(Connector connector) {
                    connector.setAttribute(ConnectorType.CONNECTOR_ATTRIBUTE_NAME, ConnectorType.externalConnector);
                    connector.setAttribute("maxThreads", Integer.parseInt(appContext.getEnvironment().getProperty("maxHttpThreads", "100")));
                    
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
