package com.axonivy.connector.kafka;

import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Default supplier for KafkaConsumer with a regex pattern topic.
 */
public class DefaultTopicConsumerSupplier<K, V> implements KafkaTopicConsumerSupplier<K, V> {

	@Override
	public KafkaConsumer<K, V> apply(Properties properties, String topic) {
		KafkaConsumer<K, V> consumer = KafkaService.get().createConsumer(properties);
		consumer.subscribe(Pattern.compile(topic));
		return consumer;
	}
}
