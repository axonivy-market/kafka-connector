# Apache Kafka Konnektor

[Apache Kafka](https://kafka.apache.org/)  ist eine verteilte Streaming-Plattform, mit der du Streams von Datensätzen veröffentlichen und abonnieren kannst. Sie ist darauf ausgelegt, große Mengen an Echtzeit-Datenströmen zu verarbeiten und kann zur Implementierung von Echtzeit-Streaming-Datenpipelines und -Anwendungen verwendet werden. Kafka wird häufig für Use-Cases wie Datenintegration, Echtzeit-Analysen und Log-Aggregation eingesetzt - v.a. immer dann, wenn große Datenmengen auftreten.

Der Apache Kafka Konnektor von Axon Ivy hilft dir, deine Prozessautomatisierung zu beschleunigen, indem er dir Zugriff auf die Feature von Apache Kafka bietet.

Dieser Konnektor:
- Basiert auf der [Apache Kafka API](https://kafka.apache.org/34/javadoc/).
- Gibt dir Zugriff auf einen oder mehrere Apache Kafka Message-Handling-Server oder -Cluster.
- Ermöglicht es dir, mehrere  Verbindungskonfigurationen zu definieren
- erstelle `KafkaConsumer` oder `KafkaProducer`
- Stellt ein `IProcessStartEventBean` zur Verfügung, das verwendet werden kann, um Ivy-Prozesse zu starten, die synchron oder asynchron Apache Kafka-Nachrichten verarbeiten
- Unterstützt dich mit einer Demo-Implementierung, um deinen Integrationsaufwand zu reduzieren
