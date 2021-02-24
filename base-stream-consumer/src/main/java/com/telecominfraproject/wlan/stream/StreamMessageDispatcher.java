package com.telecominfraproject.wlan.stream;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;
import com.telecominfraproject.wlan.core.model.streams.QueuedStreamMessage;

/**
 * @author dtop
 * Component that receives messages from stream consumers, and delivers them to all registered stream processors.
 * It is safe to push messages to this component from multiple stream consumer threads. 
 */
@Component
public class StreamMessageDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(StreamMessageDispatcher.class);

    private final TagList tags = CloudMetricsTags.commonTags;

    final Counter messagesProcessed = new BasicCounter(MonitorConfig.builder("stream-processors-messagesProcessed").withTags(tags).build());

	//autowire all available stream processors
	@Autowired(required = false)	
	List<StreamProcessor> streamProcessors;
	
    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(messagesProcessed);
    } 

	@PostConstruct
	private void postConstruct() {
		if(streamProcessors!=null && !streamProcessors.isEmpty()) {
			streamProcessors.forEach(sp -> LOG.info("Registered stream processor {}", sp) );
		} else {
			LOG.info("No registered stream processors ");
		}
	}
	
	/**
	 * Iterate through all the registered stream processors and push the incoming message into each one of them
	 * @param message
	 */
	public void push(QueuedStreamMessage message) {

		if(streamProcessors == null) {
			//nothing to do here
			return;
		}
		
		LOG.trace("Pushing message to stream processors {}", message);
		messagesProcessed.increment();
		streamProcessors.forEach(sp -> sp.push(message));
	}
}
