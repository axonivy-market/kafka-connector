package com.axonivy.connector.kafka;

import org.apache.kafka.clients.consumer.Consumer;

public interface KafkaConsumerSupplier<K, V> {
	public Consumer<K, V> supply(String configurationName);
}
