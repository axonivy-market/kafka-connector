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

### KafkaStartEventBean

The demo also contains examples of using a `KafkaStartEventBean`. Whenever
you send a message from the Demo GUI, it will be received by one of the
Demo listeners (which log them into the Runtime log).

### Other tools

On the web you will find stand-alone tools like [Offset Explorer](https://www.offsetexplorer.com) [^1] and web-based tools. These tools differ in their licence conditions, so please look at their web-pages.

[^1]: Please note the licence conditions.



## Setup

YOUR SETUP DESCRIPTION GOES HERE
Docker

```
@variables.yaml@
```

## Usage

* Classpath problem
* synchronous/asynchronous, thread pools
* Hierarchical configuration 
