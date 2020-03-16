package com.telecominfraproject.wlan.cloudmetrics.container.start;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCompositeMonitor;
import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.StepCounter;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;

@Component
@Profile("do_not_use") // will use JmxMetricsPoller instead
public class JvmStatsPublisher {
    
    private static final Logger LOG = LoggerFactory.getLogger(JvmStatsPublisher.class);

    private final TagList tags = CloudMetricsTags.commonTags;
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    
    private String[][] jmxMetricsToPoll = {
            {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","bytesReceived"},
            {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","bytesSent"},
            {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","errorCount"},
            {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","requestCount"},
            {"Tomcat:type=GlobalRequestProcessor,name=\"http-nio-9091\"","processingTime"},
            //{"Tomcat:type=Manager,host=localhost,context=/","activeSessions"}, //make sure the reported number is 0 - we do not use sessions
            {"Tomcat:type=ProtocolHandler,port=9091","connectionCount"}, 
            //{"Tomcat:type=RequestProcessor,worker=\"http-nio-9091\",name=HttpRequest1",""}, //per-worker stats
            {"Tomcat:type=ThreadPool,name=\"http-nio-9091\"","currentThreadCount"},
            {"Tomcat:type=ThreadPool,name=\"http-nio-9091\"","currentThreadsBusy"},
    };
    
    private Object getJmxAttributeValue(String objectName, String attrName){
        ObjectName name = null;
        try {
            name = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            LOG.error("Cannot read object name {}: {}", objectName, e);
            return null;
        }   
        
        try {
            return mbs.getAttribute(name, attrName);
        } catch (Exception e) {
            LOG.error("Cannot read object attribute {}.{}: {}", objectName, attrName, e);
            return null;
        }
    }
    
    private Long getJmxAttributeValueAsLong(String objectName, String attrName){ 
        Object ret = getJmxAttributeValue(objectName, attrName);
        if (ret == null) {
            return null;
        }
        if(! (ret instanceof Long)){
            LOG.error("attempted to read property {}.{} as Long, but it is of class {}", objectName, attrName, ret.getClass());
            return null;
        }
        
        return (Long) ret;
    }

    private Float getJmxAttributeValueAsFloat(String objectName, String attrName) {
        Object ret = getJmxAttributeValue(objectName, attrName);
        if (ret == null) {
            return null;
        }
        if (!(ret instanceof Float || ret instanceof Double)) {
            LOG.error("attempted to read property {}.{} as Float, but it is of class {}", objectName, attrName,
                    ret.getClass());
            return null;
        }

        return ((Number) ret).floatValue();
    }

    @PostConstruct
    public void startPublisher(){


        Thread jvmStatPublisher = new Thread(new Runnable() {
            
            boolean needToRegisterMonitors = true;
            Map<String, StepCounter> monitorsMap = new HashMap<>();
            
            @Override
            public void run() {
                
                while(true){
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(15));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    //publish metrics 
                    
                    if(needToRegisterMonitors){
                        
                        registerGcMonitors(monitorsMap);
                        registerOsMonitors();
                        
                        needToRegisterMonitors = false;
                    }

                    pollGcMetricsFromJmx(monitorsMap);
                    
                    
                }
                LOG.debug("Exited monitoring thread");
            }
        }, "jvmStatPublisher");
        
        jvmStatPublisher.setDaemon(true);
        jvmStatPublisher.start();
    }

    private void registerOsMonitors() {
        BasicGauge<Float> processCpu = new BasicGauge<>(
                MonitorConfig.builder("os-process-cpu").withTags(tags).build(), 
                new Callable<Float>() {
                    @Override
                    public Float call() throws Exception {
                        return getJmxAttributeValueAsFloat("java.lang:type=OperatingSystem", "ProcessCpuLoad")*100;
                    }
                });
        
        DefaultMonitorRegistry.getInstance().register(processCpu);

        BasicGauge<Float> systemCpu = new BasicGauge<>(
                MonitorConfig.builder("os-system-cpu").withTags(tags).build(), 
                new Callable<Float>() {
                    @Override
                    public Float call() throws Exception {
                        return getJmxAttributeValueAsFloat("java.lang:type=OperatingSystem", "SystemCpuLoad")*100;
                    }
                });
        
        DefaultMonitorRegistry.getInstance().register(systemCpu);

    }

    private void pollGcMetricsFromJmx(Map<String, StepCounter> monitorsMap) {
        List<GarbageCollectorMXBean> gcList = ManagementFactory.getGarbageCollectorMXBeans();
        for(GarbageCollectorMXBean tmpGC : gcList){

            String collCntKey = "gc-"+tmpGC.getName().replace(' ', '-')+"-collectionCount";
            StepCounter collCnt = monitorsMap.get(collCntKey);
            long incrementAmount = tmpGC.getCollectionCount() - collCnt.getValue().longValue();
            if(incrementAmount>0){
                collCnt.increment(incrementAmount);
            }

            String collTimeKey = "gc-"+tmpGC.getName().replace(' ', '-')+"-collectionTime";
            StepCounter collTime = monitorsMap.get(collTimeKey);
            incrementAmount = tmpGC.getCollectionTime() - collTime.getValue().longValue();
            if(incrementAmount>0){
                collTime.increment(incrementAmount);
            }
        }

//      System.out.println( "Memory Pools Info" );
//      List<MemoryPoolMXBean> memoryList = ManagementFactory.getMemoryPoolMXBeans();
//      for(MemoryPoolMXBean tmpMem : memoryList){
//
//          System.out.println("\nName: " + tmpMem.getName());
//          System.out.println("Usage: " + tmpMem.getUsage());
//          System.out.println("Collection Usage: " + tmpMem.getCollectionUsage());
//          System.out.println("Peak Usage: " + tmpMem.getPeakUsage());
//          System.out.println("Type: " + tmpMem.getType());
//          System.out.println("Memory Manager Names: ") ;
//
//          String[] memManagerNames = tmpMem.getMemoryManagerNames();
//          for(String mmnTmp : memManagerNames){
//              System.out.println("\t" + mmnTmp);
//          }
//          System.out.println("\n");
//      }
     
        
    }

    private void registerGcMonitors(Map<String, StepCounter> monitorsMap) {
        
        List<Monitor<?>> monitors = new ArrayList<>();

        MonitorConfig.Builder builder = MonitorConfig.builder("jvmstat");
        builder.withTag("class", "JvmStatsPublisher").withTags(tags);

        List<GarbageCollectorMXBean> gcList = ManagementFactory.getGarbageCollectorMXBeans();
        for(GarbageCollectorMXBean tmpGC : gcList){
            String collCntKey = "gc-"+tmpGC.getName().replace(' ', '-')+"-collectionCount";
            StepCounter collCnt = new StepCounter(MonitorConfig.builder(collCntKey).withTags(tags).build());
            monitorsMap.put(collCntKey, collCnt);
            monitors.add(collCnt);

            String collTimeKey = "gc-"+tmpGC.getName().replace(' ', '-')+"-collectionTime";
            StepCounter collTime = new StepCounter(MonitorConfig.builder(collTimeKey).withTags(tags).build());
            monitorsMap.put(collTimeKey, collTime);
            monitors.add(collTime);
        }

        final MemoryUsage mu =ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        final MemoryUsage muNH =ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        
        BasicGauge<Long> maxHeap = new BasicGauge<>(
                MonitorConfig.builder("memusage-heap-max").withTags(tags).build(), 
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return mu.getMax();
                    }
                });
        
        monitors.add(maxHeap);
        
        BasicGauge<Long> usedHeap = new BasicGauge<>(
                MonitorConfig.builder("memusage-heap-used").withTags(tags).build(), 
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return mu.getUsed();
                    }
                });
        
        monitors.add(usedHeap);
        
        BasicGauge<Long> committedHeap = new BasicGauge<>(
                MonitorConfig.builder("memusage-heap-committed").withTags(tags).build(), 
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return mu.getCommitted();
                    }
                });
        
        monitors.add(committedHeap);

        //
        //non-heap stats
        //
        
        BasicGauge<Long> maxNonHeap = new BasicGauge<>(
                MonitorConfig.builder("memusage-nonheap-max").withTags(tags).build(), 
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return muNH.getMax();
                    }
                });
        
        monitors.add(maxNonHeap);
        
        BasicGauge<Long> usedNonHeap = new BasicGauge<>(
                MonitorConfig.builder("memusage-nonheap-used").withTags(tags).build(), 
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return muNH.getUsed();
                    }
                });
        
        monitors.add(usedNonHeap);
        
        BasicGauge<Long> committedNonHeap = new BasicGauge<>(
                MonitorConfig.builder("memusage-nonheap-committed").withTags(tags).build(), 
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return muNH.getCommitted();
                    }
                });
        
        monitors.add(committedNonHeap);

        //
        //
        //
        
        final OperatingSystemMXBean osInfo = ManagementFactory.getOperatingSystemMXBean();
        
        BasicGauge<Double> systemLoadAvg = new BasicGauge<>(
                MonitorConfig.builder("jvm-load-avg").withTags(tags).build(), 
                new Callable<Double>() {
                    @Override
                    public Double call() throws Exception {
                        return osInfo.getSystemLoadAverage();
                    }
                });
        
        monitors.add(systemLoadAvg);


        final ThreadMXBean threadInfo = ManagementFactory.getThreadMXBean();
        
        BasicGauge<Integer> threadCount = new BasicGauge<>(
                MonitorConfig.builder("jvm-thread-count").withTags(tags).build(), 
                new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return threadInfo.getThreadCount();
                    }
                });
        
        monitors.add(threadCount);


        
        BasicCompositeMonitor bcm = new BasicCompositeMonitor(builder.build(), monitors);
        DefaultMonitorRegistry.getInstance().register(bcm);
        
    }
}
