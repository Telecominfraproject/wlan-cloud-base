package com.telecominfraproject.wlan.core.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.telecominfraproject.wlan.core.client.models.HttpClientConfig;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * @author dtoptygin
 *
 */
@Configuration
@Profile("RestTemplateConfiguration_X509_client_cert_auth")
public class RestTemplateConfigurationX509ClientCertAuth {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateConfigurationX509ClientCertAuth.class);

    @Autowired
    private Environment environment;
    @Autowired
    private HttpClientConfig httpClientConfig;
    @Autowired
    private SSLContext sslContext;

    private String subjectDn;

    /**
     * @return name of the client as stored in the X509 certificate
     */
    public String getSubjectDn() {
        return subjectDn;
    }

    @Bean
    SSLContext configureSslContext() {
        try {

            KeyStore trustStore = KeyStore.getInstance(httpClientConfig.getTruststoreType(),
                    httpClientConfig.getTruststoreProvider());
            InputStream instreamTrust = getInputStream(httpClientConfig.getTruststoreFile(environment));
            try {
                String truststorePass = HttpClientConfig.decodeStorePasswordValue(httpClientConfig.getTruststorePass(),
                        HttpClientConfig.getKeystoreEncKey(environment));
                trustStore.load(instreamTrust, truststorePass.toCharArray());
            } finally {
                instreamTrust.close();
            }

            KeyStore keystore = KeyStore.getInstance(httpClientConfig.getKeystoreType(),
                    httpClientConfig.getKeystoreProvider());

            InputStream instreamKey = getInputStream(httpClientConfig.getKeystoreFile(environment));
            String keystorePass = HttpClientConfig.decodeStorePasswordValue(httpClientConfig.getKeystorePass(),
                    HttpClientConfig.getKeystoreEncKey(environment));
            char[] keyPassword = (keystorePass == null) ? null : keystorePass.toCharArray();

            try {
                keystore.load(instreamKey, keyPassword);
            } finally {
                instreamKey.close();
            }

            // Trust own CA and all self-signed certs
            SSLContext sslCxt = SSLContexts.custom().loadTrustMaterial(trustStore,
                    // new TrustSelfSignedStrategy()
                    new TrustAllStrategy())
                    // DO NOT USE ALIAS STRATEGY - it is broken for client cert
                    // auth
                    // .loadKeyMaterial(keystore, keyPassword, new
                    // ClientKeyAliasSelector(httpClientConfig.getKeyAlias()) )
                    .loadKeyMaterial(keystore, keyPassword).useTLS().build();

            // Extract client name from the client certificate
            // and make it available externally for future reference
            X509Certificate clientCertificate = (X509Certificate) keystore
                    .getCertificate(httpClientConfig.getKeyAlias());
            
            //if cannot find specified key alias - try default ones: clientkeyalias and clientqrcode
            if(clientCertificate == null) {
            	LOG.info("Specified key alias {} not found in the keystore, will try clientkeyalias", httpClientConfig.getKeyAlias());
            	clientCertificate = (X509Certificate) keystore.getCertificate("clientkeyalias");
                if(clientCertificate == null) {
                	LOG.info("clientkeyalias not found in the keystore, will try clientqrcode");
                	clientCertificate = (X509Certificate) keystore.getCertificate("clientqrcode");
                }
            }
            
            Principal principal = clientCertificate.getSubjectDN();
            subjectDn = principal.getName();
            
            // Replace pattern-breaking characters
            subjectDn = subjectDn.replaceAll("[\n|\r|\t]", "_");

            int startPos = subjectDn.indexOf("CN=") + "CN=".length();
            int endPos = subjectDn.indexOf(',', startPos);
            subjectDn = subjectDn.substring(startPos, endPos);
            
            LOG.info("X509 client name {}", subjectDn);
            return sslCxt;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

    }

    private static InputStream getInputStream(String location) throws IOException {
        LOG.info("Loading content of {}", location);
        Object configContent = ResourceUtils.getURL(location).getContent();
        InputStream ret = null;
        if (configContent instanceof String) {
            ret = new ByteArrayInputStream(((String) configContent).getBytes(StandardCharsets.UTF_8));
        } else if (configContent instanceof InputStream) {
            ret = (InputStream) configContent;
        }
        LOG.debug("Got input stream from {}", location);

        return ret;
    }

    @Bean
    RestTemplate restTemplate() {
        try {

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

            SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).build();
            httpClientBuilder.setDefaultSocketConfig(socketConfig);

            httpClientBuilder.setDefaultConnectionConfig(ConnectionConfig.DEFAULT);

            httpClientBuilder.setMaxConnTotal(httpClientConfig.getMaxConnectionsTotal());
            httpClientBuilder.setMaxConnPerRoute(httpClientConfig.getMaxConnectionsPerRoute());
            // Override the keep alive strategy with local configuration
            // override
            if (httpClientConfig.getIdleTimeout() > 0) {
                httpClientBuilder.setKeepAliveStrategy(new KeepAliveStrategy(httpClientConfig.getIdleTimeout()));
            }

            // see https://issues.apache.org/jira/browse/HTTPCLIENT-1275
            // HostnameVerifier is an additional security check that applies to
            // trusted certificates only. It is not meant as a substitute for
            // trust verification.
            X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
            httpClientBuilder.setHostnameVerifier(hostnameVerifier);

            httpClientBuilder.setSslcontext(sslContext);

            CloseableHttpClient httpClient = httpClientBuilder.build();

            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                    httpClient);

            RestTemplate rt = new RestTemplate(requestFactory);
            ResponseErrorHandler errorHandler = new ExceptionPropagatingErrorHandler();
            rt.setErrorHandler(errorHandler);

            // this is to cover case when restTemplate is used outside of Spring
            // container
            RestTemplatePostConfiguration.registerModulesWithObjectMappers(rt);

            return rt;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    @Bean
    AsyncRestTemplate asyncRestTemplate() {
        try {

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

            SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).build();
            httpClientBuilder.setDefaultSocketConfig(socketConfig);

            httpClientBuilder.setDefaultConnectionConfig(ConnectionConfig.DEFAULT);

            int maxConnTotal = 100;
            httpClientBuilder.setMaxConnTotal(maxConnTotal);
            int maxConnPerRoute = 10;
            httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);

            // see https://issues.apache.org/jira/browse/HTTPCLIENT-1275
            // HostnameVerifier is an additional security check that applies to
            // trusted certificates only. It is not meant as a substitute for
            // trust verification.
            X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
            httpClientBuilder.setHostnameVerifier(hostnameVerifier);

            httpClientBuilder.setSslcontext(sslContext);

            CloseableHttpClient httpClient = httpClientBuilder.build();
            CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClientBuilder.create()
                    .setHostnameVerifier(hostnameVerifier).setSSLContext(sslContext).build();

            HttpComponentsAsyncClientHttpRequestFactory asyncRequestFactory = new HttpComponentsAsyncClientHttpRequestFactory(
                    httpClient, httpAsyncClient);

            AsyncRestTemplate art = new AsyncRestTemplate(asyncRequestFactory);
            ResponseErrorHandler errorHandler = new ExceptionPropagatingErrorHandler();
            art.setErrorHandler(errorHandler);

            // this is to cover case when restTemplate is used outside of Spring
            // container
            RestTemplatePostConfiguration.registerModulesWithObjectMappers(art);

            return art;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

    }
}
