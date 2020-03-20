package com.telecominfraproject.wlan.hierarchical.datastore.writer;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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
 * @author dtop
 * 
 * This class reads models from a customer_equipment queue, writes them
 * into appropriate zipStreams according to model timestamps, and
 * triggers flushes of those streams to files.
 *
 */
public class QueueReaderRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(QueueReader.class);

    private final Map<Long, StreamHolder> streamMap = new HashMap<>();
    
    private final int customerId;
    private final long equipmentId;
    private final BlockingQueue<BaseJsonModel> queue;
    private final String servoMetricPrefix;
    private final Counter processedModelCounter;
    private final HierarchicalDatastore hierarchicalDatastore;
    private final long idleTimeoutBeforeFlushingMs;

    public QueueReaderRunnable(BlockingQueue<BaseJsonModel> queue, HierarchicalDatastore hierarchicalDatastore, int customerId, long equipmentId, long idleTimeoutBeforeFlushingMs) {
        this.queue = queue;
        this.customerId = customerId;
        this.equipmentId = equipmentId;
        this.servoMetricPrefix = "hdsQueueReader-"+hierarchicalDatastore.getDsRootDirName()+"-"+hierarchicalDatastore.getDsPrefix()+"-"+hierarchicalDatastore.getFileNamePrefix()+"-";
        this.processedModelCounter = CloudMetricsUtils.getCounter(servoMetricPrefix+"processedModel-count");
        this.idleTimeoutBeforeFlushingMs = idleTimeoutBeforeFlushingMs;
        this.hierarchicalDatastore = hierarchicalDatastore;
    }
    
    @Override
    public void run() {
        try{
            LOG.info("Started QueueReader thread {} ", Thread.currentThread().getName());
            while(true){
                BaseJsonModel model = null;
                try {
                    model = queue.poll(idleTimeoutBeforeFlushingMs, TimeUnit.MILLISECONDS);
                    
                    if(model!=null){
                        LOG.trace("Got from queue({}:{}) {}", customerId, equipmentId, model);
                    }
                } catch (InterruptedException e) {
                    // do nothing
                    Thread.currentThread().interrupt();
                }
    
                if(model == QueueReader.poisonPill){
                    //stop work and exit
                    break;
                }
    
                try{
    
                    if(model==null){
                        //did not read anything from the queue after idleTimeout of waiting.
                        //Check if existing zipStreams need to be flushed into files.
                        //Although we did not read anything from the queue here, we do not want to wait too long until 
                        //the next model appears in the queue before we flush existing streams to files.
                        //That is why we are flushing to files after the idleTimeout after last model was read from queue, no matter what. 
                        commitOutputStreamsToFiles(false);
                        
                        //wait for the next message in the queue
                        continue;
                    }
    
                    if(  !(    model instanceof HasCustomerId 
                            && model instanceof HasEquipmentId 
                            && model instanceof HasProducedTimestamp
                          )){
                        LOG.debug("Not enough information to store this model {}, will skip it. Model has to provide customer id, equipment id and timestamp.", model.getClass());
                        //wait for the next message in the queue
                        continue;
                    }
    
                    long modelTs = ((HasProducedTimestamp) model).getProducedTimestampMs();
    
                    //determine the in-memory stream to write the model to
                    //first normalize timestamp to n minutes - per hDatastore configuration
                    long normalizedModelTs = modelTs - modelTs%(1L*hierarchicalDatastore.getNumberOfMinutesPerFile()*60*1000);
                    //then find the stream from the normalized timestamp
                    StreamHolder streamHolder = streamMap.get(normalizedModelTs);
                    
                    //create stream if needed - only one thread is doing this
                    if(streamHolder == null){
                        streamHolder = new StreamHolder(modelTs, customerId, equipmentId, hierarchicalDatastore);
                        streamMap.put(streamHolder.getStreamKey(), streamHolder);
                    }
    
                    streamHolder.writeModelToStream(model);
                    
                    processedModelCounter.increment();
    
                    //flush only idle streams to files
                    commitOutputStreamsToFiles(false);
                    
                }catch(Exception e){
                    LOG.error("Exception when writing into stream", e);
                }
            }
    
            //unconditionally flush the remainder of streams into files before exiting
            try {
                commitOutputStreamsToFiles(true);
            } catch (IOException e) {
                LOG.error("Exception when writing into stream", e);
            }
        }catch(Exception e){
            LOG.error("Got exception: ",e);
        }
        
        LOG.info("Thread exited {}", Thread.currentThread().getName());

    }

    /**
     * Check all open streams, and upload them to files if they were idle for longer than idleTimeoutBeforeFlushingMs.
     * @param forceFlush - if true, then unconditionally flush all existing streams to files
     * @throws IOException
     */
    private void commitOutputStreamsToFiles(boolean forceFlush) throws IOException {
        StreamHolder streamHolder;
        Map.Entry<Long, StreamHolder> mapEntry;
        Iterator<Map.Entry<Long, StreamHolder>> iter = streamMap.entrySet().iterator();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        
        while(iter.hasNext()){
            mapEntry = iter.next();
            streamHolder = mapEntry.getValue();
            if(forceFlush || (System.currentTimeMillis() - streamHolder.getLastModelWrittenToStreamTimestampMs()) >= idleTimeoutBeforeFlushingMs ){
                //stream was idle long enough, can flush it to file now
                streamHolder.commitOutputStreamToFile();
                
                //stream is uploaded to file, no need to keep it in memory anymore
                iter.remove();
                
                //now we can update fileCreatedTimestampsForInterval in hazelcast - append new timestamp for just-uploaded-stream to it 
                calendar.setTime(new Date(streamHolder.getZipStreamStartTimeMs()));
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                //int minute = calendar.get(Calendar.MINUTE);
                
                hierarchicalDatastore.appendFileNameToDirectoryListing(customerId, equipmentId, year, month, day, hour, streamHolder.getFullFileName());

            }
        }
        
    }

    

}
