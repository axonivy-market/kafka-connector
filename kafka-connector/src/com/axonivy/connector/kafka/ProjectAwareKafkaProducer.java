/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axonivy.connector.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.ProducerFencedException;


public class ProjectAwareKafkaProducer<K, V> implements Producer<K, V> {
	protected KafkaProducer<K, V> delegate;

	public ProjectAwareKafkaProducer(KafkaProducer<K, V> delegate) {
		this.delegate = delegate;
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public void initTransactions() {
		delegate.initTransactions();
	}

	@Override
	public void beginTransaction() throws ProducerFencedException {
		delegate.beginTransaction();
	}

	@Override
	public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId)
			throws ProducerFencedException {
		delegate.sendOffsetsToTransaction(offsets, consumerGroupId);
	}

	@Override
	public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets,
			ConsumerGroupMetadata groupMetadata) throws ProducerFencedException {
		delegate.sendOffsetsToTransaction(offsets, groupMetadata);
	}

	@Override
	public void commitTransaction() throws ProducerFencedException {
		delegate.commitTransaction();
	}

	@Override
	public void abortTransaction() throws ProducerFencedException {
		delegate.abortTransaction();
	}

	@Override
	public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
		return KafkaService.get().executeWithProjectClassLoader(() -> delegate.send(record));
	}

	@Override
	public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
		return KafkaService.get().executeWithProjectClassLoader(() -> delegate.send(record, callback));
	}

	@Override
	public void flush() {
		delegate.flush();
	}

	@Override
	public List<PartitionInfo> partitionsFor(String topic) {
		return delegate.partitionsFor(topic);
	}

	@Override
	public Map<MetricName, ? extends Metric> metrics() {
		return delegate.metrics();
	}

	@Override
	public Uuid clientInstanceId(Duration timeout) {
		return delegate.clientInstanceId(timeout);
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public void close(Duration timeout) {
		delegate.close(timeout);
	}
}
