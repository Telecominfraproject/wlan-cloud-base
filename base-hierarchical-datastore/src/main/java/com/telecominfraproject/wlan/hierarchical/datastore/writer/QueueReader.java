package com.telecominfraproject.wlan.hierarchical.datastore.writer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.hierarchical.datastore.HierarchicalDatastore;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * QueueReader picks up objects from the queue and writes them into zip streams in memory - one stream per N minutes, according to HierarchicalDatastore settings.<br>
 * Zip streams will be flushed into files after they have been idle for X minutes, according to HierarchicalDatastore settings.<br>
 * 
 * @author dtop
 *
 */
public class QueueReader {
    
    private static final Logger LOG = LoggerFactory.getLogger(QueueReader.class);

    @SuppressWarnings("serial")
    static final BaseJsonModel poisonPill = new BaseJsonModel(){};

    private final int customerId;
    private final long equipmentId;
    private final String fileNamePrefix;
    private final BlockingQueue<BaseJsonModel> queue = new ArrayBlockingQueue<>(5000);
    private final Thread queueReaderThread;
    private boolean shutdownRequested;
    
    public QueueReader(HierarchicalDatastore hierarchicalDatastore, int customerId, long equipmentId, long idleTimeoutBeforeFlushingMs) {
        this.customerId = customerId;
        this.equipmentId = equipmentId;
        this.fileNamePrefix = hierarchicalDatastore.getFileNamePrefix();

        queueReaderThread = new Thread(new QueueReaderRunnable(queue, hierarchicalDatastore, customerId, equipmentId, idleTimeoutBeforeFlushingMs), 
                "queueReader_"+fileNamePrefix+"_"+customerId+"_"+equipmentId+"_"+System.currentTimeMillis());
        
        //This thread has to be non-daemon because we need it alive when shutdown hook 
        // runs - to process poison pills and perform flush to files 
        queueReaderThread.setDaemon(false);
        
    }
    
    /**
     * Start reading messages from the queue
     */
    public void start(){
        queueReaderThread.start();
    }
    
    public int getQueueSize(){
        return queue.size();
    }
    
    public boolean isAlive(){
        return queueReaderThread.isAlive();
    }

    /**
     * Shutdown and flush to files all existing streams. 
     * Actual shutdown and flush is performed when poison pill is read by the queueReaderThread.
     */
    public void shutdown(){
        if(!shutdownRequested){
            shutdownRequested = true;
            LOG.info("Shutting down queue {}_{}_{}", fileNamePrefix, customerId, equipmentId);
            addToQueue(poisonPill);            
        }
    }
    
    public void addToQueue(BaseJsonModel model){
        LOG.trace("Adding model to queue({}_{}_{}) {}", fileNamePrefix, customerId, equipmentId, model);
        try {
            queue.put(model);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenericErrorException("Interrupted while trying to insert model into a queue " + fileNamePrefix + " for customer "+customerId+" equipment "+equipmentId, e);
        }
    }

    /**
     * @return true if shutdown was requested and queue reader thread have flushed all streams to S3 and exited
     */
    public boolean isShutdownCompleted() {
        return shutdownRequested && !queueReaderThread.isAlive();
    }
    
}
