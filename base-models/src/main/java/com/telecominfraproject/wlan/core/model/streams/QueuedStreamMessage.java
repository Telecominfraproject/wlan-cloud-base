package com.telecominfraproject.wlan.core.model.streams;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.json.interfaces.HasProducedTimestamp;

public class QueuedStreamMessage{
	private String topic;
	private BaseJsonModel model;
	
	public QueuedStreamMessage(String topic, BaseJsonModel model) {
		if( ! (model instanceof HasProducedTimestamp) ) {
			throw new IllegalArgumentException("model must implement HasProducedTimestamp interface in order to be queued in a Stream");
		}
		this.topic = topic;
		this.model = model;
	}
	
	public long getProducedTimestampMs() {
		return ((HasProducedTimestamp) model).getProducedTimestampMs();
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public BaseJsonModel getModel() {
		return model;
	}

	public void setModel(BaseJsonModel model) {
		this.model = model;
	}
}