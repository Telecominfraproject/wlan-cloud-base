package com.telecominfraproject.wlan.core.server.container;

import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.util.ResourceUtils;

/**
 * @author dtoptygin
 *
 */
@PropertySource({ "${ssl.props:classpath:ssl.properties}" })
public abstract class ServletContainerCustomizer implements EmbeddedServletContainerCustomizer {

    @Autowired private ApplicationContext appContext;
    
    @Primary
    @Bean ConnectorProperties connectorProperties(){
        return new ConnectorPropertiesImpl(appContext.getEnvironment());
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServletContainerCustomizer.class);
        
    // see http://docs.spring.io/spring-boot/docs/1.1.1.RELEASE/reference/htmlsingle/#howto-terminate-ssl-in-tomcat
    // see http://docs.spring.io/spring-boot/docs/1.1.1.RELEASE/reference/htmlsingle/#howto-enable-multiple-connectors-in-tomcat
    // see http://docs.spring.io/spring-boot/docs/1.1.1.RELEASE/reference/htmlsingle/#boot-features-customizing-embedded-containers

    @Override
    public void customize(ConfigurableEmbeddedServletContainer factory) {
        throw new IllegalStateException("ServletContainerCustomizer.customize method has to be overridden !!!");
    }
    
    protected void enableCompression(Connector connector) {
        //enable compression for rest services
        //to test use this command : 
        //  curl -i -H 'Accept-Encoding: gzip,deflate' http://url.to.your.server
        connector.setProperty("compression", "on");
        // Add json and xml mime types, as they're not in the mimetype list by default
        connector.setProperty("compressableMimeType", "text/html,text/xml,text/plain,text/css,text/javascript,image/svg+xml,application/json,application/xml,application/javascript,application/json;charset=UTF-8");
    }

    protected void disableSessionCookies(TomcatEmbeddedServletContainerFactory tomcatFactory) {
        //disable session cookies
        // http://tomcat.apache.org/tomcat-8.0-doc/config/context.html
        TomcatContextCustomizer tomcatContextCustomizer = new TomcatContextCustomizer() {
            @Override
            public void customize(final Context context) {
                context.setCookies(false);
                
                if(LOG.isDebugEnabled()){
                    Timer sessionCountPrinter = new Timer("Tomcat Session Printer", true);
                    sessionCountPrinter.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(context.getManager()!=null){
                                int activeSessions = context.getManager().getActiveSessions();
                                if(activeSessions>0){
                                    LOG.debug("Active tomcat sessions : {}", activeSessions);
                                }
                            }
                        }
                    }, 0, 10000);
                }
            }
        };
        
        tomcatFactory.addContextCustomizers(tomcatContextCustomizer);
    }
    
    protected Connector createSSlConnector(int serverPort, boolean useCertificateClientAuth, String sslPropPrefix) {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(serverPort);

        // For now we'll get metrics from the JMX beans.
        // This way we do not need to customize handlers and executors.
        // If we are to get metrics directly from the executor, we would need to
        // explore this:
        // connector.getProtocolHandler().getExecutor();

        customizeSslConnector(connector, useCertificateClientAuth, sslPropPrefix);

        return connector;
    }

    /**
     * See {@link https://tomcat.apache.org/tomcat-8.0-doc/config/http.html}
     * 
     * @param connector
     * @param useCertificateClientAuth
     * @param sslPropPrefix
     *            optional SSL property prefix
     */
    protected void customizeSslConnector(Connector connector, boolean useCertificateClientAuth, String sslPropPrefix) {
        //connector.setPort(serverPort); - port is configured outside of this method, usually taken from application.properties
        connector.setSecure(true);
        connector.setScheme("https");

        Environment environment = appContext.getEnvironment();
        LOG.info("Loading SSL properties from {}", environment.getProperty("ssl.props"));

        // for local-authority set up try with this
        connector.setAttribute("truststorePass", getSslProperty(environment, "truststorePass", sslPropPrefix));
        try {
            connector.setAttribute("truststoreFile",
                    ResourceUtils.getFile(getSslProperty(environment, "truststoreFile", sslPropPrefix)).getAbsolutePath());
            connector.setAttribute("truststoreType", getSslProperty(environment, "truststoreType", sslPropPrefix));
            connector.setAttribute("truststoreProvider", getSslProperty(environment, "truststoreProvider", sslPropPrefix));

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Cannot load truststore", e);
        }

        connector.setAttribute("keyAlias", getSslProperty(environment, "keyAlias", sslPropPrefix));
        connector.setAttribute("keystorePass", getSslProperty(environment, "keystorePass", sslPropPrefix));
        try {
            connector.setAttribute("keystoreFile",
                ResourceUtils.getFile(getSslProperty(environment, "keystoreFile", sslPropPrefix)).getAbsolutePath()
                );
            
            // see http://stackoverflow.com/questions/19613562/how-can-i-specify-my-keystore-file-with-spring-boot-and-tomcat?rq=1
            // see https://github.com/alx3apps/spring-embedded-tomcat/blob/master/etomcat6/src/main/java/ru/concerteza/springtomcat/etomcat6/EmbeddedTomcat.java
            
            connector.setAttribute("keystoreType", getSslProperty(environment, "keystoreType", sslPropPrefix));
            connector.setAttribute("keystoreProvider", getSslProperty(environment, "keystoreProvider", sslPropPrefix));

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Cannot load keystore", e);
        }
        
        if(useCertificateClientAuth){
            connector.setAttribute("clientAuth", "true"); // for use with X.509 certificates on the client
        } else {
            connector.setAttribute("clientAuth", "false"); //no auth, just SSL ( basic/digest/form auth can still be used - see com.telecominfraproject.wlan.core.server.security.WebSecurityConfig )
        }
         
        
        connector.setAttribute("sslProtocol", getSslProperty(environment, "sslProtocol", sslPropPrefix));
        connector.setAttribute("SSLEnabled", true);
        
        String strValue = getSslProperty(environment, "sslCiphers", sslPropPrefix);
        if (null != strValue) {
            LOG.debug("setting SSL ciphers to {}", strValue);
            connector.setAttribute("ciphers", strValue);
        }
        strValue = getSslProperty(environment, "sslUseServerCipherSuitesOrder", sslPropPrefix);
        if (null != strValue) {
            Boolean bValue = Boolean.valueOf(strValue);
            LOG.debug("setting SSL useServerCipherSuitesOrder to {}", bValue);
            connector.setAttribute("useServerCipherSuitesOrder", bValue);
        }
        
        connector.setAttribute("maxThreads", Integer.parseInt(appContext.getEnvironment().getProperty("maxHttpThreads", "100")));
        
        LOG.info("Configured http connector for port {} with {} threads", connector.getPort(), connector.getAttribute("maxThreads"));
    }

    /**
     * Get the property value from {@link Environment} in the search order of
     * <ol>
     * <li>{@linkplain propPrefix}.{@linkplain propName}
     * <li>{@linkplain propName}
     * </ol>
     * 
     * @param environment
     * @param propName
     *            property name, required.
     * @param propPrefix
     *            optional label to prefix the property name
     * @return
     */
    private String getSslProperty(Environment environment, String propName, String propPrefix) {
        String result = null;
        if (propName != null) {
            if (propPrefix != null) {
                result = environment.getProperty(propPrefix + '.' + propName);
            }

            if (result == null) {
                result = environment.getProperty(propName);
            }
        }
        return result;
    }

}
