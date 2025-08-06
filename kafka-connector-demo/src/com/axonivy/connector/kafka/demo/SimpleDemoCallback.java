package com.axonivy.connector.kafka.demo;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * A simple callback not using Ivy functionality.
 */
public class SimpleDemoCallback implements Callback {
	private long lastOffset = 0;
	private long counter = 0;
	private long exceptions = 0;
	@Override
	public void onCompletion(RecordMetadata metadata, Exception exception) {
		if(metadata != null) {
			counter++;
			lastOffset = metadata.offset();
		}

		if(exception != null) {
			exceptions++;
		}
	}
	public long getLastOffset() {
		return lastOffset;
	}
	public long getCounter() {
		return counter;
	}
	public long getExceptions() {
		return exceptions;
	}
}
