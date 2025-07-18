package com.axonivy.connector.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Default supplier for KafkaConsumer with a regex pattern topic.
 */
public class DefaultConsumerSupplier<K, V> implements KafkaConsumerSupplier<K, V> {

	@Override
	public KafkaConsumer<K, V> supply(String configurationName) {
		return KafkaService.get().consumer(configurationName);
	}
}
