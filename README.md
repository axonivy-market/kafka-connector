# kafka-connector

[![CI Build](https://github.com/axonivy-market/kafka-connector/actions/workflows/ci.yml/badge.svg)](https://github.com/axonivy-market/kafka-connector/actions/workflows/ci.yml)

Connect to Apache Kafka.

This connector gives you access to one or more Apache Kafka message
handling servers or clusters. It allows you to define multiple,
inheriting connection configurations and lets you quickly create
a `KafkaConsumer` or a `KafkaProducer`.

Additionally, this connector provides an `IProcessStartEventBean` which
can be used to start Ivy processes which react on Apache Kafka messages
synchronously or asynchronously. 

Read our [documentation](kafka-connector-product/README.md).
