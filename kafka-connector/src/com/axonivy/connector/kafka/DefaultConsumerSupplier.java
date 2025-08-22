package com.axonivy.connector.kafka;

import org.apache.kafka.clients.consumer.Consumer;

/**
 * Default supplier for Consumer with a regex pattern topic.
 */
public class DefaultConsumerSupplier<K, V> implements KafkaConsumerSupplier<K, V> {

	@Override
	public Consumer<K, V> supply(String configurationName) {
		return KafkaService.get().consumer(configurationName);
	}
}
