package com.telecominfraproject.wlan.stream;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.streams.QueuedStreamMessage;

/**
 * @author dtop
 * Parent Class for all Stream Processors.
 * 
 */
public abstract class StreamProcessor {
	
    private static final Logger LOG = LoggerFactory.getLogger(StreamProcessor.class);

	private static final int corePoolSize = Integer.getInteger("tip.wlan.streamProcessors.corePoolSize", 2);

	private static int maxPoolSize = Integer.getInteger("tip.wlan.streamProcessors.maxPoolSize", 20);

	private static long threadKeepAliveTimeSec = Long.getLong("tip.wlan.streamProcessors.threadKeepAliveTimeSec", 300);

	private static int execQueueCapacity = Integer.getInteger("tip.wlan.streamProcessors.execQueueCapacity", 500);
    
    private static Executor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize , threadKeepAliveTimeSec , TimeUnit.SECONDS, new ArrayBlockingQueue<>(execQueueCapacity ), new ThreadFactory() {		
		@Override
		public Thread newThread(Runnable r) {
			Thread thr = new Thread(r, "StreamProcessor-exec-"+System.currentTimeMillis());
			thr.setDaemon(true);
			return thr;
		}
	}); 
    		
    
	public final void push(QueuedStreamMessage message) {
		try {
			if(!acceptMessage(message)) {
				//message was not meant for this processor, do nothing
				return;
			}
					
			executor.execute( () -> processMessage(message) );
		} catch (Exception e) {
		    LOG.error("Exception when pushing message {} into {}", message, this.getClass().getName(),  e);
		}
	}

	/**
	 * Check if this stream processor is interested in the specified message.
	 * @param message
	 * @return true if this Stream Processor is interested in this message and will be processing it.
	 */
	protected abstract boolean acceptMessage(QueuedStreamMessage message);
	
	/**
	 * Perform application-specific processing of the incoming message - react to it immediately, place it into internal queues, etc. 
	 * This method will be executed asynchronously. 
	 * @param message
	 */
	protected abstract void processMessage(QueuedStreamMessage message);
	
}
