package com.axonivy.connector.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;

public interface KafkaConsumerSupplier<K, V> {
	public KafkaConsumer<K, V> supply(String configurationName);
}
