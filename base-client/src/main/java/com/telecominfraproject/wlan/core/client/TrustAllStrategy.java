package com.telecominfraproject.wlan.core.client;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.TrustStrategy;

public class TrustAllStrategy implements TrustStrategy {

    @Override
    public boolean isTrusted(final X509Certificate[] chain,
            final String authType) throws CertificateException { 
        // do not verify host names - for use with local-ca and self-signed certificates
        return true;
    }

}
