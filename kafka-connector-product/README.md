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

### Apache Kafka in Docker

If you do not have access to an existing Apache Kafka, you may quickly start one
in a docker container. You may want to use the provided docker compose file
[docker-compose.yml](files/docker-compose.yml) as a starter.

Copy this file to your machine and `cd`to the directory. Enter there the command
```
docker-compose up -d
```
and docker will start a `zookeeper` server on port 2181  and a `kafka` server on port 9092. To
connect to this server, use `localhost:9092` as your bootstrap server. Note, that the demo
is configred to connect exactly to this server.

## Usage

The connector was built to give you as much access as possible to the original Apache Kafka API
while providing some useful semantics to use in Axon Ivy.

### KafkaStartEvenBean

An `IProcessStartEvenBean` named `KafkaStartEventBean` is provided to listen to topics and start
Ivy processes. This bean needs to be configured:

Topic Pattern
: Enter a valid `java.util.regex.Pattern` as the topic to listen to. Note, that words without
special chararcters are valid patterns. So there is no need to learn a special syntax to listen
to simple topic names. Topic patterns are case sensitive

Synchronous
: When a message is received, will the bean wait until the started process gives back control
(synchronous) or continue to receive messages in parallel (asynchronous).


* Classpath problem
* synchronous/asynchronous, thread pools
* Hierarchical configuration 
