package com.telecominfraproject.wlan.core.client.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.text.StrLookup;
import org.apache.commons.text.StrSubstitutor;
import org.springframework.core.env.Environment;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.utils.TokenEncoder;
import com.telecominfraproject.wlan.core.model.utils.TokenUtils;

/**
 * @author dtoptygin
 *
 */
public class HttpClientConfig extends BaseJsonModel {
    private static final long serialVersionUID = -7880727024040943516L;
    /**
     * Password obfuscate prefix
     */
    private static final String OBF_PREFIX = "OBF:";
    /**
     * Prefix 4 bytes random text to password before encode
     */
    private static final int RANDOM_LENGTH = 4;
    
    private static final Random RANDOM = new Random();
    
    /**
     * Environment property to encrypt password store key
     */
    public static final String HTTP_CLIENT_ENC_KEY_PROP = "tip.wlan.httpClient.encKey";

    public static String decodeStorePasswordValue(String password, String key) {
        String result = null;
        if (password != null) {
            if (password.startsWith(OBF_PREFIX)) {
                result = TokenUtils.decrypt(password.substring(OBF_PREFIX.length()), key,
                        TokenEncoder.Base62TokenEncoder);
                if (result != null) {
                    result = result.substring(RANDOM_LENGTH);
                }
            }
            else {
                result = password;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        HttpClientConfig c = new HttpClientConfig();
        c.maxConnectionsTotal = 100;
        c.maxConnectionsPerRoute = 10;
        c.idleTimeout = 60000;
        c.truststoreFile = "";
        c.truststorePass = "";
        c.truststoreProvider = "";
        c.truststoreType = "";
        c.credentialsList = new ArrayList<>();

        HttpClientCredentials cred1 = new HttpClientCredentials();
        cred1.setHost("localhost");
        cred1.setPort(9090);
        cred1.setUser("user");
        cred1.setPassword("password");

        HttpClientCredentials cred2 = new HttpClientCredentials();
        cred2.setHost("localhost");
        cred2.setPort(9095);
        cred2.setUser("user");
        cred2.setPassword("password");

        c.credentialsList.add(cred1);
        c.credentialsList.add(cred2);

        // System.out.println(c);
    }

    /**
     * Utility to generate obfuscated value for store password
     * 
     * @param password
     * @param key
     * @return
     */
    public static String obfStorePasswordValue(String password, String key) {
        String result = null;
        if (password != null) {
            result = TokenUtils.encrypt(prefixRandomValue(password), key, TokenEncoder.Base62TokenEncoder);
        }
        return (result == null) ? result : OBF_PREFIX + result;
    }

    private static String prefixRandomValue(String password) {
        String prefix = String.format("%04x", RANDOM.nextInt(0xFFFF));
        return prefix.substring(0, RANDOM_LENGTH) + password;
    }

    private int maxConnectionsTotal;
    private int maxConnectionsPerRoute;

    /**
     * number of seconds before server closes the connection. value less than 1
     * means not set.
     */
    private int idleTimeout = 0;
    /**
     * for connect and read timeouts value of 0 (default) means infinite
     */
    private int connectTimeoutMs;
    private int readTimeoutMs;
    private String truststoreType;

    private String truststoreProvider;
    private String truststoreFile;
    private String truststorePass;
    // these properties describe keystore that contains client keys for X509
    // certificate-based auth
    private String keystoreType;
    private String keystoreProvider;

    private String keystoreFile;

    private String keystorePass;

    private String keyAlias;

    private List<HttpClientCredentials> credentialsList;

    @Override
    public HttpClientConfig clone() {
        return (HttpClientConfig) super.clone();
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public List<HttpClientCredentials> getCredentialsList() {
        return credentialsList;
    }

    public int getIdleTimeout() {
        return this.idleTimeout;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public String getKeystoreFile(Environment environment) {
        return resolveStoreFile(environment, getKeystoreFile());
    }
    
    /**
     * Grab the keystore encryption key. can be overriden use {@value #HTTP_CLIENT_ENC_KEY_PROP}
     * @param environment
     * @return
     */
    public static String getKeystoreEncKey(Environment environment) {
        return environment.getProperty(HTTP_CLIENT_ENC_KEY_PROP, "kod@Cl0ud20190817");
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public String getKeystoreProvider() {
        return keystoreProvider;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public int getMaxConnectionsTotal() {
        return maxConnectionsTotal;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public String getTruststoreFile(Environment environment) {
        return resolveStoreFile(environment, getTruststoreFile());
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public String getTruststoreProvider() {
        return truststoreProvider;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public void setCredentialsList(List<HttpClientCredentials> credentialsList) {
        this.credentialsList = credentialsList;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }

    public void setKeystoreProvider(String keystoreProvider) {
        this.keystoreProvider = keystoreProvider;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public void setMaxConnectionsTotal(int maxConnectionsTotal) {
        this.maxConnectionsTotal = maxConnectionsTotal;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public void setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
    }

    public void setTruststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
    }

    public void setTruststoreProvider(String truststoreProvider) {
        this.truststoreProvider = truststoreProvider;
    }

    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }
    
    /**
     * Support variable expansion.
     * 
     * @param environment
     *            use system property if this is null.
     * @param fileValue
     * @return
     */
    private String resolveStoreFile(Environment environment, String fileValue) {
        if (fileValue != null) {
            StrLookup<String> variableResolver;
            if (environment != null) {
                variableResolver = new StrLookup<String>() {
                    public String lookup(String key) {
                        return environment.getProperty(key);
                    }
                };
            } else {
                variableResolver = StrLookup.systemPropertiesLookup();
            }
            StrSubstitutor substituor = new StrSubstitutor(variableResolver);
            substituor.setEnableSubstitutionInVariables(true);
            return substituor.replace(fileValue);
        }
        return fileValue;
    }

    /**
     * Use {@link #getKeystoreFile(Environment)}
     * 
     * @return
     */
    protected String getKeystoreFile() {
        return keystoreFile;
    }

    /**
     * use {@link #getTruststoreFile(Environment)}
     * 
     * @return raw value
     */
    protected String getTruststoreFile() {
        return truststoreFile;
    }
}
