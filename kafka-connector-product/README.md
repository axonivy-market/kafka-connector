# Apache Kafka Connector

Connect to Apache Kafka.

This connector gives you access to one or more Apache Kafka message
handling servers or clusters. It allows you to define multiple,
inheriting connection configurations and lets you quickly create
a `KafkaConsumer` or a `KafkaProducer`.

Additionally, this connector provides an `IProcessStartEventBean` which
can be used to start Ivy processes which react on Apache Kafka messages
synchronously or asynchronously. 

## Demo

The demo provides a single dialog with quick sending buttons for different topics.
You may enter a key and a value but it is ok, to use the same key and value for
multiple messages.

To see the effect of sending messages you have multiple options:

### Apache Kafka command line

Start a console consumer and see messages appearing on the console directly.

```
kafka-console-consumer --bootstrap-server localhost:9092 --topic TopicA
```
Type `kafka-console-consumer --help` for usage.



## Setup

YOUR SETUP DESCRIPTION GOES HERE
Docker

```
@variables.yaml@
```
