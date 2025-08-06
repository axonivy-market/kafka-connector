# kafka-connector

[![CI Build](https://github.com/axonivy-market/kafka-connector/actions/workflows/ci.yml/badge.svg)](https://github.com/axonivy-market/kafka-connector/actions/workflows/ci.yml)

This connector gives you access to one or more Apache Kafka message
handling servers. It allows you to define multiple, inheriting connection configurations
and lets you quickly create a `Consumer` or a `Producer`.

Additionally, this connector provides an `IProcessStartEventBean` which
can be used to start Ivy processes which react on Apache Kafka messages
synchronously or asynchronously and supported schema registries. 

Read our [documentation](kafka-connector-product/README.md).
