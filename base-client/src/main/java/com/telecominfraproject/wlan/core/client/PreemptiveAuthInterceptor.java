package com.telecominfraproject.wlan.core.client;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yongli
 *
 */
public class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

    
    private RestHttpClientConfig config;
    
    public PreemptiveAuthInterceptor(RestHttpClientConfig config) {
        this.config = config;
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(PreemptiveAuthInterceptor.class);
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.
     * HttpRequest, org.apache.http.protocol.HttpContext)
     */
    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);

        // If no auth scheme available yet, try to initialize it
        // preemptively
        if (authState.getAuthScheme() == null) {
            CredentialsProvider credsProvider = (CredentialsProvider) context
                    .getAttribute(HttpClientContext.CREDS_PROVIDER);
            HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
            
            if ((null != config) && !config.isPreemptive(targetHost)) {
                LOG.debug("Skipping preemptive authentication for target {}", targetHost);
                return;
            }
            Credentials creds = credsProvider
                    .getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
            if (creds == null) {
                LOG.error("No credentials for preemptive authentication for target {}", targetHost);
                throw new HttpException("No credentials for preemptive authentication");
            }
            
            authState.update(new BasicScheme(), creds);
        }
    }

}