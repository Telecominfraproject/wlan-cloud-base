package com.telecominfraproject.wlan.cloudmetrics;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Gauge;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.tag.TagList;

/**
 * @author dtop
 *
 */
public class CloudMetricsUtils {

    private static final TagList tags = CloudWatchTags.commonTags;

    private static final Map<String, Timer> timersMap = new ConcurrentHashMap<>();
    private static final Map<String, Counter> countersMap = new ConcurrentHashMap<>();
    private static final Map<String, Gauge<Long>> gaugesMap = new ConcurrentHashMap<>();

    public static Timer getTimer(String timerMetricId) {
        Timer tmr = timersMap.get(timerMetricId);
        
        if(tmr==null){
            synchronized(timersMap){
                tmr = timersMap.get(timerMetricId);
                if(tmr==null){
                    tmr = new BasicTimer(MonitorConfig.builder(timerMetricId).withTags(tags).build());
                    DefaultMonitorRegistry.getInstance().register(tmr);
                    timersMap.put(timerMetricId, tmr);
                }
            }
        }
        
        return tmr;
    }

    public static  Counter getCounter(String counterMetricId) {
        Counter cnt = countersMap.get(counterMetricId);
        
        if(cnt==null){
            synchronized(countersMap){
                cnt = countersMap.get(counterMetricId);
                if(cnt==null){
                    cnt = new BasicCounter(MonitorConfig.builder(counterMetricId).withTags(tags).build());
                    DefaultMonitorRegistry.getInstance().register(cnt);
                    countersMap.put(counterMetricId, cnt);
                }
            }
        }
        
        return cnt;
    } 
    
    public static Gauge<Long> registerGauge(String gaugeMetricId, Callable<Long> callable){
        Gauge<Long> gauge = gaugesMap.get(gaugeMetricId);
        
        if(gauge==null){
            synchronized(gaugesMap){
                gauge = gaugesMap.get(gaugeMetricId);
                if(gauge==null){
                    gauge = new BasicGauge<>(
                        MonitorConfig.builder(gaugeMetricId).withTags(tags).build(), callable);
                    DefaultMonitorRegistry.getInstance().register(gauge);
                    gaugesMap.put(gaugeMetricId, gauge);
                }
            }
        }
        
        return gauge;
    }

}
