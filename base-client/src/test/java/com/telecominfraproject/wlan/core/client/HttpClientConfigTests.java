package com.telecominfraproject.wlan.core.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import com.telecominfraproject.wlan.core.client.models.HttpClientConfig;

public class HttpClientConfigTests {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientConfigTests.class);
    private static final String KEY = "fastTestKey12345";
    private static final String PASSWORD = "toptopsecret";

    @Test
    public void testClientLoadWithoutProperty() throws Exception {
        HttpClientConfigResolver resolver = new HttpClientConfigResolver();
        HttpClientConfig config = resolver.configureHttpClientConfig(null);
        assertNotNull(config);
        String resolvedPath = config.getTruststoreFile(null);
        assertNotNull(resolvedPath);
        LOG.debug("getTruststoreFile({}) got {}, loading from {}", config, resolvedPath,
                System.getProperty("user.dir"));
        Object content = ResourceUtils.getURL(resolvedPath).getContent();
        assertNotNull(content);
    }

    @Test
    public void testClientLoadWithProperty() throws Exception {
        System.setProperty("CONF_FOLDER_KEY", "src/test/resources");
        HttpClientConfigResolver resolver = new HttpClientConfigResolver();
        HttpClientConfig config = resolver.configureHttpClientConfig(null);
        assertNotNull(config);
        String resolvedPath = config.getKeystoreFile(null);
        assertNotNull(resolvedPath);
        LOG.debug("getKeystoreFile({}) got {}, loading from {}", config, resolvedPath, System.getProperty("user.dir"));
        Object content = ResourceUtils.getURL(resolvedPath).getContent();
        assertNotNull(content);
    }

    @Test
    public void testStorePassword() {
        Set<String> testResults = new HashSet<>();
        for (int i = 0; i < 10; ++i) {
            String obfValue = HttpClientConfig.obfStorePasswordValue(PASSWORD, KEY);
            LOG.debug("obfStorePasswordValue: {}", obfValue);
            assertFalse("unique value", testResults.contains(obfValue));
            testResults.add(obfValue);
            assertEquals("decode " + obfValue, PASSWORD, HttpClientConfig.decodeStorePasswordValue(obfValue, KEY));
        }
    }
}
