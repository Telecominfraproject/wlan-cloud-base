/**
 * 
 */
package com.whizcontrol.core.server.container;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import com.whizcontrol.core.model.json.BaseJsonModel;
import com.whizcontrol.server.exceptions.ConfigurationException;

/**
 * @author yongli
 * @param <C>
 *            configuration
 *
 */
public abstract class BaseConfigResourceProvider<C extends BaseJsonModel> {

    private static final Logger LOG = LoggerFactory.getLogger(BaseConfigResourceProvider.class);

    /**
     * Configuration location
     */
    private final String configLocation;
    private C loadedConfig;
    private Boolean currentLoading = false;
    private Lock loadingLock = new ReentrantLock();
    Long lastLoadTime = 0L;
    private final long reloadDuration;

    /**
     * Return Logger
     * 
     * @return
     */
    protected abstract Logger getLogger();

    /**
     * Name of the configuraiton used for logging
     * 
     * @return
     */
    protected abstract String getConfigurationName();

    /**
     * Constructor
     * 
     * @param configLocation
     */
    protected BaseConfigResourceProvider(String configLocation, long reloadDuration) {
        this.configLocation = configLocation;
        this.reloadDuration = reloadDuration;
    }

    protected C getConfig(Class<C> clazz) {
        if (System.currentTimeMillis() > lastLoadTime + this.reloadDuration) {
            try {
                return loadConfiguration(clazz);
            } catch (Exception e) {
                // ignore it
                LOG.warn("Failed to load {} due to exception", clazz.getSimpleName(), e);
            }
        }
        return loadedConfig;
    }

    /**
     * Load the configuration
     * 
     * @throws ConfigurationException
     * 
     * @return loaded configuration
     * 
     */
    protected C loadConfiguration(Class<C> clazz) {
        try {
            loadingLock.lock();
            if (!currentLoading) {
                try {
                    currentLoading = true;
                    lastLoadTime = System.currentTimeMillis();
                    getLogger().info("Loading {} from {}", getConfigurationName(), configLocation);
                    Object configContent = ResourceUtils.getURL(configLocation).getContent();
                    C config;
                    if (configContent instanceof InputStream) {
                        config = BaseJsonModel.fromString(
                                StreamUtils.copyToString((InputStream) configContent, StandardCharsets.UTF_8), clazz);
                    } else {
                        config = BaseJsonModel.fromString((String) configContent, clazz);
                    }
                    getLogger().info("Got {} configuration from {}", getConfigurationName(), configLocation);
                    loadedConfig = config;
                } catch (Exception e) {
                    throw new ConfigurationException("Failed to load Configuration " + getConfigurationName(), e);
                } finally {
                    currentLoading = false;
                }
            }
            return loadedConfig;
        } finally {
            loadingLock.unlock();
        }
    }
}
