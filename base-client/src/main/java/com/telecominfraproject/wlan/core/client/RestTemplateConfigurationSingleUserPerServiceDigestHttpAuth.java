package com.telecominfraproject.wlan.core.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.concurrent.Callable;

import javax.net.ssl.SSLContext;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudWatchTags;
import com.telecominfraproject.wlan.core.client.models.HttpClientConfig;
import com.telecominfraproject.wlan.core.client.models.HttpClientCredentials;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * @author dtoptygin
 * This class can be used for communicating with servers using both http-basic and http-digest auth.
 * 
 */
@Configuration
@Profile("rest-template-single-user-per-service-digest-auth")
public class RestTemplateConfigurationSingleUserPerServiceDigestHttpAuth {

    private final TagList tags = CloudWatchTags.commonTags;

    private static final Logger LOG = LoggerFactory
            .getLogger(RestTemplateConfigurationSingleUserPerServiceDigestHttpAuth.class);

    @Autowired
    private HttpClientConfig httpClientConfig;
    @Autowired
    private SSLContext sslContext;
    @Autowired
    private RestHttpClientConfig restHttpClientConfig;

    @Bean
    SSLContext configureSslContext(Environment environment) {
        try {
            
            if(httpClientConfig==null){
                //cover the case when this code is executed outside of Spring container
                httpClientConfig = new HttpClientConfigResolver().configureHttpClientConfig(environment);
            }

            if (httpClientConfig.getTruststoreType() == null
                    || httpClientConfig.getTruststoreType().isEmpty()) {
                return null;
            }

            KeyStore trustStore = KeyStore.getInstance(
                    httpClientConfig.getTruststoreType(),
                    httpClientConfig.getTruststoreProvider());
            String truststoreFile = httpClientConfig.getTruststoreFile(environment);
            InputStream instreamTrust = getInputStream(truststoreFile);
            try {
                LOG.info("Loading truststore for client connection from {}", truststoreFile);

                trustStore.load(instreamTrust, httpClientConfig
                        .getTruststorePass().toCharArray());
            } finally {
                instreamTrust.close();
            }

            // Trust own CA and all self-signed certs
            SSLContext sslCxt = SSLContexts.custom()
                    .loadTrustMaterial(trustStore,
                    // new TrustSelfSignedStrategy()
                            new TrustAllStrategy()).useTLS().build();

            return sslCxt;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * This method is in here to fix a bug in ResourceUtils.getFile(String resourceLocation)
     * - because that one does not handle "classpath:" urls properly
     * @param fileOrClasspathUrl
     * @return
     * @throws IOException
     */
    private InputStream getInputStream(String fileOrClasspathUrl) throws IOException {
        InputStream ret;
        if (fileOrClasspathUrl.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            String path = fileOrClasspathUrl.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
            String description = "class path resource [" + path + "]";
            ClassLoader cl = ClassUtils.getDefaultClassLoader();
            URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
            if (url == null) {
                throw new FileNotFoundException(
                        description + " cannot be resolved to absolute file path " +
                        "because it does not reside in the file system");
            }
            
            return url.openStream();
        } else {
            File trustStoreFile = new File(ResourceUtils.getFile(
                    fileOrClasspathUrl).getAbsolutePath());
            ret = new FileInputStream(trustStoreFile);
        }
        
        return ret;
    }

    @Bean(name="RestTemplate")
    public RestOperations restTemplateBean(Environment environment) {
        //dtop: wrap restTemplate into a proxy for generating JMX metrics
//        RestOperations ret = TimedInterface.newProxy(RestOperations.class, newRestTemplate(), "RestTemplate");
//        DefaultMonitorRegistry.getInstance().register((CompositeMonitor)ret);

        //RestOperations ret = new RestOperationsWithMetrics(newRestTemplate());
        
        //each remote client will wrap this with RestOperationsWithMetrics
        RestOperations ret = newRestTemplate(environment);
        
        return ret;

    }
    
    //dtop: do not expose this as a bean - in order to enable monitoring it has to be wrapped. @see restTemplateBean()
    public RestTemplate newRestTemplate(Environment environment) {
        try {

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoKeepAlive(true).build();
            httpClientBuilder.setDefaultSocketConfig(socketConfig);

            ConnectionConfig connectionConfig = ConnectionConfig.DEFAULT;
            httpClientBuilder
                    .setDefaultConnectionConfig(connectionConfig);
            
            if(httpClientConfig==null){
                //cover the case when this code is executed outside of Spring container
                httpClientConfig = new HttpClientConfigResolver().configureHttpClientConfig(environment);
            }

            httpClientBuilder.setMaxConnTotal(httpClientConfig
                    .getMaxConnectionsTotal());
            httpClientBuilder.setMaxConnPerRoute(httpClientConfig
                    .getMaxConnectionsPerRoute());
            // Override the keep alive strategy with local configuration
            // override
            if (httpClientConfig.getIdleTimeout() > 0) {
                httpClientBuilder.setKeepAliveStrategy(new KeepAliveStrategy(
                        httpClientConfig.getIdleTimeout()));
            }
            
            // This is only needed for basic/digest auth
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            // Read all necessary credentials from httpClientConfig object that
            // is read from file (and later - from config service)
            if (httpClientConfig.getCredentialsList() != null) {
                for (HttpClientCredentials httpClientCredentials : httpClientConfig
                        .getCredentialsList()) {
                    credentialsProvider.setCredentials(new AuthScope(
                            httpClientCredentials.getHost(),
                            httpClientCredentials.getPort()),
                            new UsernamePasswordCredentials(
                                    httpClientCredentials.getUser(),
                                    httpClientCredentials.getPassword()));
                }
            }

            httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider);

            if (httpClientConfig.getTruststoreType() == null
                    || httpClientConfig.getTruststoreType().isEmpty()) {
                LOG.info("NOT using SSL for the restTemplate");
            }
            // see https://issues.apache.org/jira/browse/HTTPCLIENT-1275
            // HostnameVerifier is an additional security check that applies to
            // trusted certificates only. It is not meant as a substitute for
            // trust verification.
            X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
            httpClientBuilder.setHostnameVerifier(hostnameVerifier);

            if(sslContext==null){
                //cover the case when this code is executed outside of Spring container
                sslContext = configureSslContext(environment);
            }
            httpClientBuilder.setSslcontext(sslContext);
            
            //have to do this - to get a reference to connection manager, otherwise no way to get inside connection pool metrics
            final PoolingHttpClientConnectionManager httpClientConnectionManager
                    = configureClientConnectionManager(httpClientBuilder, hostnameVerifier, sslContext, 
                            socketConfig, connectionConfig,
                            httpClientConfig.getMaxConnectionsTotal(),
                            httpClientConfig.getMaxConnectionsPerRoute()
                            );

            // To prevent a bug on large file uploads, always send authentication headers 
            // to ensure the client does not have to rely on a CHALLENGE response from the
            // server. When the files get too large, the CHALLENGE response may be missed 
            // due to inability of the client to clear the buffer.
            httpClientBuilder.addInterceptorFirst(new PreemptiveAuthInterceptor(restHttpClientConfig));
            
            //at last - build the client
            final CloseableHttpClient httpClient = httpClientBuilder.build();

            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                    httpClient);
            
            int connectTimeoutMs = httpClientConfig.getConnectTimeoutMs();
            if(connectTimeoutMs>0){
                requestFactory.setConnectTimeout(connectTimeoutMs);
            }
            int readTimeoutMs = httpClientConfig.getReadTimeoutMs();
            if(readTimeoutMs>0){
                requestFactory.setReadTimeout(readTimeoutMs);
            }
            
            //
            //wire in connection manager metrics
            //

            BasicGauge<Integer> availableConnectionsCount = new BasicGauge<>(
                    MonitorConfig.builder("http-available-conn-count").withTags(tags).build(), 
                    new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return httpClientConnectionManager.getTotalStats().getAvailable();
                        }
                    });
            
            DefaultMonitorRegistry.getInstance().register(availableConnectionsCount);

            BasicGauge<Integer> leasedConnectionsCount = new BasicGauge<>(
                    MonitorConfig.builder("http-leased-conn-count").withTags(tags).build(), 
                    new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return httpClientConnectionManager.getTotalStats().getLeased();
                        }
                    });
            
            DefaultMonitorRegistry.getInstance().register(leasedConnectionsCount);

            BasicGauge<Integer> pendingConnectionsCount = new BasicGauge<>(
                    MonitorConfig.builder("http-pending-conn-count").withTags(tags).build(), 
                    new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return httpClientConnectionManager.getTotalStats().getPending();
                        }
                    });
            
            DefaultMonitorRegistry.getInstance().register(pendingConnectionsCount);

            
            RestTemplate rt = new RestTemplate(requestFactory);
            ResponseErrorHandler errorHandler = new ExceptionPropagatingErrorHandler();
            rt.setErrorHandler(errorHandler );
            
            //this is to cover case when restTemplate is used outside of Spring container
            RestTemplatePostConfiguration.registerModulesWithObjectMappers(rt);
            
            return rt;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * 
     * see HttpClientBuilder.build() - the logic is taken from there
     * 
     * @param httpClientBuilder
     * @param hostnameVerifier
     * @param sslcontext
     * @param socketConfig
     * @param connectionConfig
     * @param maxConnTotal
     * @param maxConnPerRoute
     * @return connectionManager which is plugged into httpClientBuilder
     */
    private PoolingHttpClientConnectionManager configureClientConnectionManager(HttpClientBuilder httpClientBuilder, 
            X509HostnameVerifier hostnameVerifier, SSLContext sslcontext, SocketConfig socketConfig, 
            ConnectionConfig connectionConfig, int maxConnTotal, int maxConnPerRoute) {

        final String[] supportedProtocols = null;
        final String[] supportedCipherSuites = null;
        
        LayeredConnectionSocketFactory sslSocketFactory = null;
        if(sslcontext!=null){
            sslSocketFactory = new SSLConnectionSocketFactory(
                        sslcontext, supportedProtocols, supportedCipherSuites, hostnameVerifier);
        }
        

        RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory());
        
        if(sslcontext!=null){
            socketFactoryRegistryBuilder.register("https", sslSocketFactory);
        }
        
        final PoolingHttpClientConnectionManager poolingmgr = new PoolingHttpClientConnectionManager(
                socketFactoryRegistryBuilder.build() );
        
        poolingmgr.setDefaultSocketConfig(socketConfig);
        poolingmgr.setDefaultConnectionConfig(connectionConfig);

        if (maxConnTotal > 0) {
            poolingmgr.setMaxTotal(maxConnTotal);
        }
        if (maxConnPerRoute > 0) {
            poolingmgr.setDefaultMaxPerRoute(maxConnPerRoute);
        }
        
        httpClientBuilder.setConnectionManager(poolingmgr);
        
        return poolingmgr;
    }

}
