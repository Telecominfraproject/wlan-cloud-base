package com.telecominfraproject.wlan.cloudmetrics.container.start;

import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.JmxMetricPoller;
import com.netflix.servo.publish.JvmMetricPoller;
import com.netflix.servo.publish.LocalJmxConnector;
import com.netflix.servo.publish.MemoryMetricObserver;
import com.netflix.servo.publish.MetricFilter;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.publish.cloudwatch.CloudWatchMetricObserver;
import com.netflix.servo.tag.aws.AwsInjectableTag;
import com.telecominfraproject.wlan.cloudmetrics.observers.ElasticSearchMetricObserver;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * @author dtop
 * see https://github.com/Netflix/servo/wiki for documentation and examples
 */
@Component
//@EnableScheduling
public class StartServoMetricsCollector implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(StartServoMetricsCollector.class);


    @Autowired MemoryMetricObserver inMemoryObserver;
    @Autowired Environment environment;
    @Autowired ApplicationContext applicationContext;

    @Bean
    MemoryMetricObserver memoryMetricObserver(){
        return new MemoryMetricObserver("stats", 100);
    }
    
//  {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","bytesReceived"},
    private static final Set<String> jmxTomcatGlobalRequestProcessorAttrs = 
            new HashSet<>(Arrays.asList(
                    "bytesReceived",
                    "bytesSent",
                    "errorCount",
                    "requestCount",
                    "processingTime"
            ));

//  {"Tomcat:type=ThreadPool,name=\"http-nio-9091\"","currentThreadCount"},
    private static final Set<String> jmxTomcatThreadPoolAttrs = 
            new HashSet<>(Arrays.asList(
                    "currentThreadCount",
                    "currentThreadsBusy"
            ));

    @Override
    public void run(String... args) throws Exception {
        
        long samplingIntervalMs = environment.getProperty("whizcontrol.servo.samplingIntervalMs", Long.class, TimeUnit.MINUTES.toMillis(1));
        int aggregationHeartbeatMultiplier = environment.getProperty("whizcontrol.servo.aggregationHeartbeatMultiplier", Integer.class, 2);
                
        PollScheduler scheduler = PollScheduler.getInstance();
        if(!scheduler.isStarted()){
            scheduler.start();
        }

        List<MetricObserver> observers = new ArrayList<>();

        MetricObserver transformInMemory = new CounterToRateMetricTransform(
            inMemoryObserver, aggregationHeartbeatMultiplier * samplingIntervalMs, TimeUnit.MILLISECONDS);
        observers.add(transformInMemory);
        
        registerCloudWatchIfNeeded(observers, samplingIntervalMs, aggregationHeartbeatMultiplier);

        registerElasticSearchIfNeeded(observers, samplingIntervalMs, aggregationHeartbeatMultiplier);

        PollRunnable regularMetricsPoll = new PollRunnable(
            new MonitorRegistryMetricPoller(),
            BasicMetricFilter.MATCH_ALL,
            true,
            observers);
        
        scheduler.addPoller(regularMetricsPoll, samplingIntervalMs, TimeUnit.MILLISECONDS);

        if("true".equalsIgnoreCase(environment.getProperty("whizcontrol.enableCloudWatch", "false"))
              || environment.acceptsProfiles("cloud-metrics-elastic-search", "cloud-metrics-aws-elastic-search") ){
            
            //poll for these metrics only when there's an external system available to publish them into
            
            configureJvmMetricsPoller(scheduler, observers, samplingIntervalMs);
            
            configureJmxMetricsPoller(scheduler, observers, samplingIntervalMs);

        }

    }

    private void configureJmxMetricsPoller(PollScheduler scheduler, List<MetricObserver> observers, long samplingIntervalMs) throws MalformedObjectNameException {
        //
        // Add metrics that are polled from JMX attributes
        //

        //JMX Beans matching these patterns will be polled
        ObjectName[] jmxObjectNamePatterns = new ObjectName[]{
                new ObjectName("Tomcat:type=GlobalRequestProcessor,name=\"http-nio-*\",*"),
                new ObjectName("Tomcat:type=ProtocolHandler,port=*,*"), 
                new ObjectName("Tomcat:type=ThreadPool,name=\"http-nio-*\",*"),
        };

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        List<ObjectName> jmxObjectNameFilters = new ArrayList<>();
        
        //Servo JmxMetricsPoller expects concrete names in the filter, not patterns.
        //So we are expanding patterns into real object names that match them.
        for(ObjectName namePattern: jmxObjectNamePatterns){
            Set<ObjectName> beanNames = mbs.queryNames(null, namePattern);
            jmxObjectNameFilters.addAll(beanNames);
        }

        if(LOG.isDebugEnabled()){
            for(ObjectName on: jmxObjectNameFilters){
                LOG.debug("Metrics will be polled from JMXBean {}", on.getCanonicalName());
            }
        }

        //
        //For the reference - here are the object names and their attributes we are polling:
        //
        //      {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","bytesReceived"},
        //      {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","bytesSent"},
        //      {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","errorCount"},
        //      {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","requestCount"},
        //      {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","processingTime"},
        //      //{"Tomcat:type=Manager,host=localhost,context=/","activeSessions"}, //make sure the reported number is 0 - we do not use sessions
        //      {"Tomcat:type=ProtocolHandler,port=9091","connectionCount"}, 
        //      //{"Tomcat:type=RequestProcessor,worker=\"http-nio-9091\",name=HttpRequest1",""}, //per-worker stats
        //      {"Tomcat:type=ThreadPool,name=\"http-nio-9091\"","currentThreadCount"},
        //      {"Tomcat:type=ThreadPool,name=\"http-nio-9091\"","currentThreadsBusy"},

        //metrics matching this filter will be treated as counters, all others will be gauges
        MetricFilter countersMetricFilter = new MetricFilter() {
            @Override
            public boolean matches(MonitorConfig config) {
                String domain = config.getTags().getValue("JmxDomain");
                if("Tomcat".equals(domain)){
                    String type = config.getTags().getValue("Jmx.type");
                    String attrName = config.getName();
                    if(type!=null && attrName!=null){
                        switch (type) {
                        case "GlobalRequestProcessor":
                            return true;
                        case "ProtocolHandler":
                        case "ThreadPool":
                            return false;
                        default:
                            break;
                        }
                    }
                }
                return true;
            }
        };
        
        //only JMX attributes matching this filter will be polled
        MetricFilter jmxMetricFilter = new MetricFilter() {
            @Override
            public boolean matches(MonitorConfig config) {
                String domain = config.getTags().getValue("JmxDomain");

                if ("Tomcat".equals(domain)) {
                    String name = config.getTags().getValue("Jmx.name");
                    String type = config.getTags().getValue("Jmx.type");
                    String attrName = config.getName();
                    if (type != null && attrName != null) {
                        switch (type) {
                        case "GlobalRequestProcessor":
                            return name != null && name.startsWith("\"http-nio-")
                                    && jmxTomcatGlobalRequestProcessorAttrs.contains(attrName);
                        case "ProtocolHandler":
                            return "connectionCount".equals(attrName);
                        case "ThreadPool":
                            return name != null && name.startsWith("\"http-nio-")
                                    && jmxTomcatThreadPoolAttrs.contains(attrName);
                        default:
                            break;
                        }
                    }
                }
                return false;
            }
        };
        
        PollRunnable jmxMetricsPoll = new PollRunnable(
                new JmxMetricPoller(new LocalJmxConnector(), jmxObjectNameFilters , countersMetricFilter),
                jmxMetricFilter,
                true,
                observers);
            
        scheduler.addPoller(jmxMetricsPoll, samplingIntervalMs, TimeUnit.MILLISECONDS);
        
    }

    private void configureJvmMetricsPoller(PollScheduler scheduler, List<MetricObserver> observers, long samplingIntervalMs) {
        
        //
        // Standard JVM metrics are polled in here. Internally they are retrieved from JMX
        //
        
        PollRunnable jvmMetricsPoll = new PollRunnable(
                new JvmMetricPoller(),
                BasicMetricFilter.MATCH_ALL,
                true,
                observers);
            
        scheduler.addPoller(jvmMetricsPoll, samplingIntervalMs, TimeUnit.MILLISECONDS);
        
    }

    private void registerCloudWatchIfNeeded(List<MetricObserver> observers, long samplingIntervalMs, int aggregationHeartbeatMultiplier) {
        if("true".equalsIgnoreCase(environment.getProperty("whizcontrol.enableCloudWatch", "false"))){
            throw new ConfigurationException("AWS CloudWatch should be used with extreme care. Very expensive!!! Before using - make sure that only a small number of metrics is published in there.");
        } else {
            LOG.info("AWS CloudWatch metrics collection is OFF");
        }
    }

    /**
     * This must not be used in real deployment!!!
     * @param observers
     * @param samplingIntervalMs
     * @param aggregationHeartbeatMultiplier
     */
    @SuppressWarnings("unused")
    private void turnOnCloudWatch(List<MetricObserver> observers, long samplingIntervalMs, int aggregationHeartbeatMultiplier) {
        if(!"undefined".equals(AwsInjectableTag.INSTANCE_ID.getValue())){
            LOG.info("AWS deployment - turning on AWS metrics collection");

            MetricObserver cwObserver = new CloudWatchMetricObserver(
                    environment.getProperty("app.name", "no_name"),
                    "Art2Wave/CloudWatch",
                    new DefaultAWSCredentialsProviderChain());
            
            MetricObserver transformCloudWatch = new CounterToRateMetricTransform(
                    cwObserver, aggregationHeartbeatMultiplier * samplingIntervalMs, TimeUnit.MILLISECONDS);
            observers.add(transformCloudWatch);
        } else {
            LOG.info("Local deployment - turning off AWS metrics collection");
        }
    }

    private void registerElasticSearchIfNeeded(List<MetricObserver> observers, long samplingIntervalMs, int aggregationHeartbeatMultiplier) {
        if(environment.acceptsProfiles("cloud-metrics-elastic-search", "cloud-metrics-aws-elastic-search")){
            try{
                MetricObserver elasticSearchObserver = new ElasticSearchMetricObserver(applicationContext);
                
                MetricObserver transformElasticSearch = new CounterToRateMetricTransform(
                        elasticSearchObserver, aggregationHeartbeatMultiplier * samplingIntervalMs, TimeUnit.MILLISECONDS);
                observers.add(transformElasticSearch);
            }catch(UnknownHostException e){
                LOG.error("Cannot initialize ElasticSearch client", e);
            }
        } else {
            LOG.info("ElasticSearch metrics collection is OFF");
        }
    }
}
