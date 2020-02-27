package com.whizcontrol.cloudmetrics.observers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.DynamicCounter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.StepCounter;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.tag.aws.AwsInjectableTag;
import com.whizcontrol.cloudmetrics.CloudWatchTags;


public class ElasticSearchMetricObserver extends BaseMetricObserver {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchMetricObserver.class);
            
    private final static int batchSize = 100;
    private final TransportClient transportClient;
    private final BulkProcessor bulkProcessor;
    
    private final String hostName;
    private final String appName;

    /**
     * Number of elasticsearch metrics reported.
     */
    private static final Counter METRICS_COUNTER = new StepCounter(
        new MonitorConfig.Builder("servo-elasticsearch-metrics").build());

    /**
     * Number of elasticsearch put calls.
     */
    private static final Timer PUTS_TIMER = new BasicTimer(
        new MonitorConfig.Builder("servo-elasticsearch-puts").build());

    /**
     * Number of elasticsearch errors.
     */
    private static final MonitorConfig ERRORS_COUNTER_ID =
        new MonitorConfig.Builder("servo-elasticsearch-errors").build();

    static {
      DefaultMonitorRegistry.getInstance().register(METRICS_COUNTER);
      DefaultMonitorRegistry.getInstance().register(PUTS_TIMER);
    }
        
    public ElasticSearchMetricObserver(ApplicationContext applicationContext) throws UnknownHostException {
        super("cm-"+InetAddress.getLocalHost().getHostName()+"-"+applicationContext.getEnvironment().getProperty("app.name", "no_name"));
        this.hostName = InetAddress.getLocalHost().getHostName();
        this.appName = applicationContext.getEnvironment().getProperty("app.name", "no_name");
        
        transportClient = applicationContext.getBean("cloudMetricsTransportClient", TransportClient.class);
        bulkProcessor = BulkProcessor.builder(
                transportClient,  
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId,
                                           BulkRequest request) {
                        LOG.trace("Storing {} items as part of execId {}", request.numberOfActions(), executionId);
                    } 

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          BulkResponse response) {  
                        
                        if(response.hasFailures()){
                            LOG.trace("Failures when storing items as part of execId {}", executionId);
                        }
                    } 

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable failure) {  
                        LOG.error("Bulk write failed for execId {} : {}", executionId, failure.getLocalizedMessage());
                    } 
                })
                .setBulkActions(1000) 
                .setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB)) 
                .setFlushInterval(TimeValue.timeValueMinutes(1)) 
                .setConcurrentRequests(2) 
                .build();
                
    }

    /* (non-Javadoc)
     * @see com.netflix.servo.publish.BaseMetricObserver#updateImpl(java.util.List)
     */
    @Override
    public void updateImpl(List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }

        List<Metric> batch = new ArrayList<>(batchSize);
        int batchCount = 1;

        while (!metrics.isEmpty()) {
            Metric m = metrics.remove(0);
            if (m.hasNumberValue()) {
                batch.add(m);

                if (batchCount % batchSize == 0) {
                    batchCount++;
                    putMetricData(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            putMetricData(batch);
        }

    }

    private void putMetricData(List<Metric> batch) {
        METRICS_COUNTER.increment(batch.size());
        final Stopwatch s = PUTS_TIMER.start();
        XContentBuilder metricContent = null;
        try {
            String indexName = this.getName();
//            if(!AwsInjectableTag.INSTANCE_ID.getValue().equals("undefined")){
//                indexName += AwsInjectableTag.INSTANCE_ID.getValue();
//            }
//            
//            if(!CloudWatchTags.commonTags.getValue("a2wTag").equals("undefined-undefined-undefined")){
//                indexName += CloudWatchTags.commonTags.getValue("a2wTag");
//            }
            
            indexName = indexName.toLowerCase();
            
            // see https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/bulk.html
            for(Metric m: batch){

                metricContent = XContentFactory.jsonBuilder().startObject()
                        .field("mVf", m.getNumberValue().floatValue())
                        .field("mVi", m.getNumberValue().longValue())
                        .field("mTs", m.getTimestamp())
                        .field("mApp", appName)
                        .field("mHost", hostName)
                        .field("mAppHost", appName+"_"+hostName)
                        ;
                
                if(AwsInjectableTag.INSTANCE_ID.getValue() != "undefined"){
                    metricContent.field("instanceId", AwsInjectableTag.INSTANCE_ID.getValue());
                }
                
                if(CloudWatchTags.role != "undefined"){
                    metricContent.field("role", CloudWatchTags.role);
                }

                if(CloudWatchTags.stack != "undefined"){
                    metricContent.field("stack", CloudWatchTags.stack);
                }

                if(CloudWatchTags.deployment != "undefined"){
                    metricContent.field("deployment", CloudWatchTags.deployment);
                }
                
//                String tagValue = m.getConfig().getTags().getValue("a2wTag");
//                if(tagValue!=null && !tagValue.equals("undefined-undefined-undefined")){
//                    metricContent.field("a2wTag", tagValue);
//                }
                
                String tagValue = m.getConfig().getTags().getValue("class");
                if(tagValue!=null){
                    
                    //clean up class name
                    int pos = tagValue.indexOf('$');
                    if(pos>0){
                        tagValue = tagValue.substring(0, pos);
                    }
                    
                    metricContent.field("metricClass", tagValue);
                }
                
                tagValue = m.getConfig().getTags().getValue("id");
                if(tagValue!=null){
                    metricContent.field("metricId", tagValue);
                }
                
                
                metricContent.endObject();
                
                bulkProcessor.add(
                    new IndexRequest(indexName, 
                                    getMetricType(m), 
                                    String.valueOf(m.getTimestamp())
                        ).source(
                                metricContent 
                                ));
                metricContent.close();
            }
            
            //System.err.println("PUBLISHED TO ELASTICSEARCH "+ batch.size());
        } catch (Exception e) {
            final Tag error = new BasicTag("error", e.getClass().getSimpleName());
            DynamicCounter.increment(ERRORS_COUNTER_ID.withAdditionalTag(error));
            LOG.error("Error while submitting data for metrics : " + batch, e);
            if (metricContent != null) {
                metricContent.close();
            }
        } finally {
            s.stop();
        }
    }
    
    /**
     * @param metric
     * @return string that represents flattened-out metric type to be stored in elasticsearch
     */
    private static String getMetricType(Metric m){
        StringBuilder mType = new StringBuilder(256);
        TagList tags = m.getConfig().getTags();
        String id = tags.getValue("id");
        String mClass = tags.getValue("class");
        String mName = m.getConfig().getName();
        String mStat = tags.getValue("statistic");
        
        if(mClass!=null){
            //clean up class name
            int pos = mClass.indexOf('$');
            if(pos>0){
                mClass = mClass.substring(0, pos);
            }
            
            mType.append(shortenName(mClass)).append("-");
        }
        
        if(id!=null){
            if(mClass==null || !id.equals(mClass)){
                //if class and id are the same - do not repeat them
                mType.append(shortenName(id)).append("-");
            }
        }
        
        //cover the case of JMX metrics
        String jmxDomain = tags.getValue("JmxDomain");
        String jmxType = tags.getValue("Jmx.type");
        String jmxName = tags.getValue("Jmx.name");
        
        if(jmxDomain!=null){
            mType.append(jmxDomain).append("-");
            
            if(jmxType!=null){
                mType.append(jmxType).append("-");
            }

            if(jmxName!=null){
                jmxName = jmxName.replace("\"", "");
                mType.append(jmxName).append("-");
            }
            
        }

        //cover thread state tag
        String jmxState = tags.getValue("state");
        if(jmxState!=null){
            mType.append(jmxState).append("-");
        }

        
        mType.append(mName);
        
        if(mStat!=null){
            mType.append("-").append(mStat);
        }
        
        return mType.toString().toLowerCase().replace(' ', '-');
    }

    private static String shortenName(String mClass) {
        switch(mClass){
        case "OperatingSystemMXBean": return "os";
        case "GarbageCollectorMXBean": return "gc";
        case "MemoryPoolMXBean": return "mem";
        case "AsyncConfiguration": return "async";
        case "CompilationMXBean": return "compile";
        case "ClassLoadingMXBean": return "cl";
        case "ThreadMXBean": return "thr";
        case "CamiWebSocketHandler": return "ws";
        case "RealTimeDataAnalyticsAsyncConfiguration": return "rtasync";
        case "SingleDataSourceConfig": return "ds";
        }
        
        return mClass;
    }


}
