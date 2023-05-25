package com.axonivy.connector.kafka;

import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;

public interface KafkaTopicConsumerSupplier<K, V> {
	public KafkaConsumer<K, V> apply(Properties properties, String topic);
}
