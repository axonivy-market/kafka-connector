package com.axonivy.connector.kafka;

import java.util.function.BiFunction;

import org.apache.kafka.clients.consumer.KafkaConsumer;

public interface KafkaTopicConsumerSupplier<K, V> extends BiFunction<String, String, KafkaConsumer<K, V>>{

}
