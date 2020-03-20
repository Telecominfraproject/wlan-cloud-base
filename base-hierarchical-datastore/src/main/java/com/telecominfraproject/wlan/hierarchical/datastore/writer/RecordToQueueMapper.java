package com.telecominfraproject.wlan.hierarchical.datastore.writer;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.servo.monitor.Counter;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsUtils;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.json.interfaces.HasCustomerId;
import com.telecominfraproject.wlan.core.model.json.interfaces.HasEquipmentId;
import com.telecominfraproject.wlan.core.model.json.interfaces.HasProducedTimestamp;
import com.telecominfraproject.wlan.hierarchical.datastore.HierarchicalDatastore;

/**
 * This component takes incoming BaseJsonModel, extracts partitioning key from
 * it (usually customerId_equipmentId), and delivers BaseJsonModel into
 * appropriate queue.<br>
 * QueueReaders will pick up objects from the queues and write them into zip streams in memory.<br>
 * Zip stream will be flushed into file every n-minute - according to hDatastore configuration.<br>
 * 
 * This object instance is shared between all threads that read from kafka. 
 * 
 * @author dtop
 *
 */
public class RecordToQueueMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RecordToQueueMapper.class);

    private Map<String, QueueReader> partitionedQueues = new ConcurrentHashMap<>();

    private final HierarchicalDatastore hierarchicalDatastore;
    private final String servoMetricPrefix;
    private final Counter addModelCounter;  
    private boolean shutdownRequested;

    public RecordToQueueMapper(HierarchicalDatastore hierarchicalDatastore) {
        this.hierarchicalDatastore = hierarchicalDatastore;
        this.servoMetricPrefix = "hdsQueueReader-"+hierarchicalDatastore.getDsRootDirName()+"-"+hierarchicalDatastore.getDsPrefix()+"-"+hierarchicalDatastore.getFileNamePrefix()+"-";
        this.addModelCounter = CloudMetricsUtils.getCounter(servoMetricPrefix+"addModel-count");
        
        CloudMetricsUtils.registerGauge(servoMetricPrefix+"numQueues", 
                new Callable<Long>(){
                    @Override
                    public Long call() throws Exception {
                        return (long) partitionedQueues.size();
                    }
                });
        
        CloudMetricsUtils.registerGauge(servoMetricPrefix+"totalQueueSize", 
                new Callable<Long>(){
                    @Override
                    public Long call() throws Exception {
                        long s = 0;
                        try{
                            for(QueueReader qr: partitionedQueues.values()){
                                s+=qr.getQueueSize();
                            }
                        }catch(Exception e){
                            //ignore it, will repeat at next metrics poll cycle
                        }
                        return s;
                    }
                });
        
        CloudMetricsUtils.registerGauge(servoMetricPrefix+"numDeadThreads", 
                new Callable<Long>(){
                    @Override
                    public Long call() throws Exception {
                        long s = 0;
                        try{
                            for(QueueReader qr: partitionedQueues.values()){
                                if(!qr.isAlive()){
                                    s++;
                                }
                            }
                        }catch(Exception e){
                            //ignore it, will repeat at next metrics poll cycle
                        }
                        return s;
                    }
                });

    }
    
    public void addModel(BaseJsonModel model){
        
        if(  !(    model instanceof HasCustomerId 
                && model instanceof HasEquipmentId 
                && model instanceof HasProducedTimestamp
              )){
            LOG.debug("Not enough information to store this model {}, will skip it. Model has to provide customer id, equipment id and timestamp.", model.getClass());
            //wait for the next message in the queue
            return;
        }
        
        int customerId = ((HasCustomerId)model).getCustomerId();
        long equipmentId = ((HasEquipmentId)model).getEquipmentId();
        
        String partitionKey = Integer.toString(customerId) + "_" + Long.toString(equipmentId);
        
        QueueReader queueReader = partitionedQueues.get(partitionKey);
        if(queueReader == null){
            synchronized(partitionedQueues){
                queueReader = partitionedQueues.get(partitionKey);
                if(queueReader==null){
                    queueReader = new QueueReader(hierarchicalDatastore, customerId, equipmentId, hierarchicalDatastore.getIdleTimeoutBeforeFlushingMs());
                    partitionedQueues.put(partitionKey, queueReader);
                    queueReader.start();
                }
            }
        }

        addModelCounter.increment();
        queueReader.addToQueue(model);
    }
    
    /**
     * Shutdown and flush to files all existing queue readers
     */
    public void shutdown(){
        if(!shutdownRequested){
            shutdownRequested = true;
            for(QueueReader qReader: partitionedQueues.values()){
                qReader.shutdown();
            }
        }
    }

    public boolean isShutdownRequested(){
        return shutdownRequested;
    }
    
    /**
     * @return true if shutdown was requested and all queue readers have flushed to files
     */
    public boolean isShutdownCompleted() {
        if(!shutdownRequested){
            return false;
        }
        
        boolean shutdownDone = true;
        for(Map.Entry<String,QueueReader> entry: partitionedQueues.entrySet()){
            shutdownDone = shutdownDone && entry.getValue().isShutdownCompleted();
            LOG.info("Shutdown status for QueueReader {} : {}", entry.getKey(), entry.getValue().isShutdownCompleted());
            if(!shutdownDone){
                break;
            }
        }
        
        return shutdownDone;
    }
}
