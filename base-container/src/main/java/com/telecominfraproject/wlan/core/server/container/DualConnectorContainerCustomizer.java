package com.telecominfraproject.wlan.core.server.container;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author dtoptygin
 *
 */
@Component
@Profile(value = { "use_ssl_with_client_cert_and_digest_auth","use_ssl_with_client_cert_and_basic_auth" })
public class DualConnectorContainerCustomizer extends ServletContainerCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(DualConnectorContainerCustomizer.class);
    
    @Autowired private ConnectorProperties connectorProperties;
    
    @Override
    public void customize(ConfigurableEmbeddedServletContainer factory) {
        
        if(factory instanceof TomcatEmbeddedServletContainerFactory) {
            TomcatEmbeddedServletContainerFactory tomcatFactory = (TomcatEmbeddedServletContainerFactory) factory;
            
            //tomcat factory will create by default only one Http connector - on the port configured in application.properties (server.port)
            //customize that connector to support SSL
            //NOTE: this affects only first, default one, connector
            tomcatFactory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
                @Override
                public void customize(Connector connector) {
                    connector.setAttribute(ConnectorType.CONNECTOR_ATTRIBUTE_NAME, ConnectorType.externalConnector);
                    customizeSslConnector(connector, true, "external");
                    
                    //this one is for websocket connections, no need for compression here
                }
            });
            
            Connector secondConnector = createSSlConnector(connectorProperties.getInternalPort(), false, "internal");
            secondConnector.setAttribute(ConnectorType.CONNECTOR_ATTRIBUTE_NAME, ConnectorType.internalConnector);
            enableCompression(secondConnector);

            tomcatFactory.addAdditionalTomcatConnectors(secondConnector);
            //default timeout is 30 minutes, connection is dropped after that by the server
            tomcatFactory.setSessionTimeout(-1);

            disableSessionCookies(tomcatFactory);
            
        } else {
            LOG.error("Cannot customize embedded container: " + factory);
        }
    }
    

}
