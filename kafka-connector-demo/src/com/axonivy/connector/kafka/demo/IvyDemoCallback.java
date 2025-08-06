package com.axonivy.connector.kafka.demo;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

import com.axonivy.connector.kafka.KafkaService;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Callback using Ivy.
 * 
 * <p>
 * Callbacks wanting to use Ivy must be created with {@link KafkaService#ivyCallback(Callback)}.
 * </p> 
 */
public class IvyDemoCallback implements Callback {
	@Override
	public void onCompletion(RecordMetadata metadata, Exception exception) {
		Ivy.log().info("Callback was called.");
		Ivy.wf().signals().create().data("Metadata: %s, Exception: %s".formatted(metadata, exception)).send("kafka:connector:demo:signal");
	}
}
