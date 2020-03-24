package com.telecominfraproject.wlan.hazelcast;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.GroupProperty;

@Configuration
@Profile("HazelcastForUnitTest")
public class HazelcastForUnitTest {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastForUnitTest.class);

    /**
     * Use this to register instance for shutdown. Each test case should have
     * its own HazelcastInstance and register it with the Manager.
     * 
     * 
     * @author yongli
     *
     */
    public static class HazelcastUnitTestManager {
        final Set<String> instanceSet = new HashSet<>();

        public void registerInstance(HazelcastInstance hazelcastInstance) {
            this.instanceSet.add(hazelcastInstance.getName());
        }

        public void shutdownAllInstances() {
            for (String instanceName : instanceSet) {
                HazelcastInstance instance = Hazelcast.getHazelcastInstanceByName(instanceName);
                if (null != instance) {
                    instance.shutdown();
                }
            }
            instanceSet.clear();
        }

        /**
         * Set system property such that the test case will have it's own
         * HazelcastInstance.
         * 
         * @param testClass
         */
        public static void initializeSystemProperty(Class<?> testClass) {
            int hashCode = testClass.hashCode();
            System.setProperty("tip.wlan.hazelcast.groupName", "wc-dev" + hashCode);
            System.setProperty("tip.wlan.hazelcast.groupPassword", "wc-dev-pass" + hashCode);
        }
    }

    @Bean
    public HazelcastInstance hazelcastInstanceTest() {
        // this is used for experiments and unit tests
        Config config = new Config();
        config.setProperty(GroupProperty.LOGGING_TYPE.getName(), "slf4j");
        config.setProperty(GroupProperty.PHONE_HOME_ENABLED.getName(), "false");
        
        GroupConfig groupConfig = new GroupConfig(System.getProperty("tip.wlan.hazelcast.groupName", "wc-dev"));
        groupConfig.setPassword(System.getProperty("tip.wlan.hazelcast.groupPassword", "wc-dev-pass"));
        config.setGroupConfig(groupConfig);
        config.getNetworkConfig().setPublicAddress("127.0.0.1").setPort(5900).setPortAutoIncrement(true)
                .setInterfaces(new InterfacesConfig().addInterface("127.0.0.1"));

        JoinConfig joinCfg = new JoinConfig();
        joinCfg.setMulticastConfig(new MulticastConfig().setEnabled(false));
        joinCfg.setTcpIpConfig(new TcpIpConfig().setEnabled(true));

        MapConfig mapConfigWildcard = new MapConfig();
        mapConfigWildcard.setName("ree-*").setBackupCount(0).setTimeToLiveSeconds(10 * 60);
        config.addMapConfig(mapConfigWildcard);
        mapConfigWildcard = new MapConfig();
        mapConfigWildcard.setName("se-*").setBackupCount(0).setTimeToLiveSeconds(10 * 60);
        config.addMapConfig(mapConfigWildcard);
        mapConfigWildcard = new MapConfig();
        mapConfigWildcard.setName("sm-*").setBackupCount(0).setTimeToLiveSeconds(10 * 60);
        config.addMapConfig(mapConfigWildcard);

        QueueConfig queueConfig = new QueueConfig();
        queueConfig.setName("commands-q-*").setBackupCount(0).setMaxSize(5000);
        config.addQueueConfig(queueConfig);

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        LOG.info("Configured Hazelcast Instance {} for unit tests", hazelcastInstance.getName());
        return hazelcastInstance;
    }
}
