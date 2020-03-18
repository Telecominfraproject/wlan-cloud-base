/**
 * 
 */
package com.telecominfraproject.wlan.core.server.container;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This stop the tmpwatch from deleting ServerRoot and DocRoot from a running
 * server.
 * 
 * @author yongli
 *
 */
@Component
@EnableScheduling
public class ServerRootWatcher implements ApplicationListener<ContextRefreshedEvent> {

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        LOG.debug("Processing ContextRefreshedEvent for {}", context.getClass().getSimpleName());

        if (!WebServerApplicationContext.class.isInstance(context)) {
            return;
        }
        WebServer container = ((WebServerApplicationContext) context).getWebServer();
        if (!TomcatWebServer.class.isInstance(container)) {
            return;
        }
        Tomcat tomcat = ((TomcatWebServer) container).getTomcat();
        for (Container child : tomcat.getHost().findChildren()) {
            if (!StandardContext.class.isInstance(child)) {
                continue;
            }
            registerServerRoot(((StandardContext) child).getDocBase());
            registerServerRoot(((StandardContext) child).getWorkPath());
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerRootWatcher.class);

    private Map<String, Long> serverRootMap = new ConcurrentHashMap<>();

    protected void registerServerRoot(String serverRoot) {
        LOG.debug("registerServerRoot({})", serverRoot);
        if (null != serverRoot) {
            if (!serverRoot.startsWith(TMP_DIR)) {
                LOG.debug("Ignore server root registartion for {} because it's not inside {}", serverRoot, TMP_DIR);
            }

            if (null == serverRootMap.putIfAbsent(serverRoot, System.currentTimeMillis())) {
                LOG.info("Registered server root {} for watch", serverRoot);
            } else {
                LOG.debug("Duplicated server root registration {}", serverRoot);
            }
        }
    }

    /**
     * Refresh the server root directory every 12 hours
     */
    @Scheduled(initialDelay = 10 * 60 * 1000, fixedRate = 12 * 60 * 60 * 1000)
    public void refreshServerRoot() {
        for (Entry<String, Long> entry : serverRootMap.entrySet()) {
            String serverRoot = entry.getKey();
            LOG.info("Refresh server root {}", serverRoot);
            File file = new File(entry.getKey());
            if (file.exists()) {
                if (!file.setLastModified(System.currentTimeMillis())) {
                    LOG.info("Failed to refresh server root {}, will retry later", serverRoot);
                }
            } else {
                LOG.warn("Won't refresh server root {} because it no longer exists", entry.getKey(), TMP_DIR);
            }
        }
    }
}
