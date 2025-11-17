# Apache #Kafka Anschluss

[Apache #Kafka](https://kafka.apache.org/) ist ein verteilt strömen Bahnsteig
jener erlaubt du zu verlegen und abonnieren zu Ströme von Schallplatten. Es ist
gestaltet zu bedienen große Inhalte von real-Zeit Daten strömen und können sein
benutzt zu implementieren real-Zeit Strömung #Daten Pipelines und Anträge.
#Kafka ist gewöhnlich für Nutzung benutzt Fälle wie Daten Integration, real-Zeit
#Analytik, und Log Ansammlung - besonders als große Beträge von Daten sind
involviert.

Das Apache #Kafka Anschluss von #Axon #Ivy hilft du beschleunigst mal eure
Arbeitsgang Automatisierung versehen Zugang zu den Charakterzügen von Apache
#Kafka.

Dieser Anschluss:

- Ist gegründet weiter das [Apache #Kafka
  API](https://kafka.apache.org/34/javadoc/).
- Versieht du mit greifst zu zu eins oder #mehr Apache #Kafka Meldung-bedienend
  Server oder Trauben.
- Unterstützt Schema #Standesamt.
- Erlaubt du zu definieren mehrfache Zusammenhang Konfigurationen.
- Schafft `Konsumenten` oder `Produzenten` Instanzen.
- Bietet an ein `IProcessStartEventBean` jener kann sein benutzt zu starten #Ivy
  verarbeitet jenen Arbeitsgang Apache #Kafka Meldungen #synchron oder
  #asynchron.
- Unterstützt du mit eine Demo Ausführung zu heruntersetzen eure Integration
  Anstrengung.

## Demo

Die Demo versieht ein Zwiegespräch mit Knöpfe für senden Meldungen zu
verschieden Apache #Kafka *Gegenstände*. Du darfst betreten ein `Schlüssel` und
einen `Wert` aber es ist ok, zu benutzen ebensolchen gleichen Schlüssel und
schätzen für mehrfach Meldungen.

*PersonTopic* Ist ein spezieller Gegenstand #welche sendet und empfängt `Person`
Objekte, mit einem Schema beliefert #bei einem Schema-#Standesamt. Note, dass
diese Demo bedürft Zugang zu einem Schema-#Standesamt. Die Hafenarbeiter
Einrichtung kann vorausgesetzt sein benutzt zu testen das Schema #Standesamt
Integration.

Zu sehen den Effekt von senden Meldungen du hast mehrfache Optionen:

### Apache #Kafka gebietet Linie

Starte einen Konsole Konsumenten auf die Befehl Linie von einer Maschine
gekoppelt zu der Apache #Kafka Server und sehen Meldungen fungieren direkt auf
der Konsole.

```
kafka-console-consumer --bootstrap-server localhost:9092 --topic TopicA
```
Typ `kafka-Konsole-Konsument --Hilfe` für Gebrauch.

### KafkaStartEventBean

Die Demo zügelt auch Beispiele von benutzen #ein `KafkaStartEventBean`. #Wann
immer sendest du eine Meldung von die Demo #grafische Benutzeroberfläche, es
will sein empfangen bei #man von die Demo Zuhörer (#welche #loggen jene hinein
das Laufzeit Log).

### Anderen Tools

Auf dem Web willst du finden Stand-#allein Tools mögen [Verschiebung
Forscher](https://www.offsetexplorer.com) oder Web-basisbezogene Tools. Bitte
#beachten die Schein Zustände.

## Einrichtung

### Apache #Kafka in Hafenarbeiter

Ob du hast nicht Zugang zu ein #existierend Apache #Kafka Server, aber hat
Hafenarbeiter installierte, du kannst starten keine Beispiel Einrichtung
einschließlich ein Schema #Standesamt benutzen mal unser Beispiel
`Hafenarbeiter-abfassen.yml` Datei in das Demo Projekt.

Kopier diese Datei zu einem Telefonbuch auf eurer Maschine, `cd` zu jenem
Telefonbuch und betreten den Befehl:

```
docker-compose up -d
```

Hafenarbeiter will starten ein `Zoowärter` Server auf #backbordseitig `2181`,
einen `kafka` Server auf #backbordseitig `9092` und ein `Schema-#Standesamt`
Server auf #backbordseitig `9081`. Zu koppeln zu das kafka Server, Nutzung
`localhost:9092` da #urladen eure Server und `http://localhost:9081` da eure
Schema #Standesamt Server. Note, dass die Demo ist konfiguriert zu benutzen
diese Server aus der Schachtel.

## Gebrauch

Der Anschluss war gebaut du #soviel Zugang zu geben da möglich zu dem Original
[Apache #Kafka API](https://kafka.apache.org/34/javadoc/) #während versehend
einige nützlichen Semantiken für Nutzung in den #Axon Efeu Umwelt.

Alle Funktionalität ist #darlegen herein `KafkaService` oder in
Ersatz-Arbeitsgang von diesem Anschluss. Die Anschluss Angebote Aufgaben zu
sicher schaffen `Konsumenten`s und `Produzenten`s weiter basisbezogen global
veriable Konfigurationen. `Produzent`s ist #zwischengespeichert und re-benutzt
für Effizienz Gründe. `Konsument`s ist nicht #zwischengespeichert. Der beste Weg
zu konsumieren Meldungen sind zu benutzen das #versehen `KafkaStartEventBean`
#welche wollen benutzen einen Single `Konsumenten` für #zuhören auf ein
Gegenstand Muster.

### Senden

Objekte können sein gesandt benutzen das #versehen senden sup-Arbeitsgang oder
direkt mal benutzen den `sendet` Annehmlichkeit Aufgabe versah mal die
`KafkaService` Da definiert mal den #Kafka API, das Resultat von *#Senden* eine
Meldung zu den #Kafka Server kann sein überwacht in zwei Wege.

1. Senden versieht ein `Zukunft` #welche du kannst `bekommst()` sofort
   (#Einfrieren) oder an einem späteren Punkt #fristgemäß.
2. Senden akzeptiert ein optionales `Rückruf` belieferte #bei dem Anrufer.

Diese Anschluss Unterstützungen sowohl Optionen und wollen benutzen die
`Rückruf` ob ihm ist nicht `Null`.

Note, jene Rückrufe rennen herein #ein `Garn` geschafft bei #Kafka und will
nicht haben Zugang zu Efeu Funktionalität. Eine Annehmlichkeit Aufgabe
`KafkaService.ivyCallback()` Ist versehen zu schaffen Rückrufe mit greifen zu zu
die Efeu Umwelt (#abgesehen zu Funktionalität #welche ist erzählt zu der
gängigen Bitte da diese Bitte ist nicht gültige Außenseite von das Bitte Garn).

Ob du möchtest benutzen senden Rückrufe mit Efeu Funktionalität solltest du
wahrscheinlich gerade senden ein Signal und füllen den komplexeren Efeu in einem
getrennten Efeu meldet Anwender. Note: Die Annehmlichkeit Aufgabe benutzt
zurzeit eine nicht-Öffentlichkeit Efeu API.

### Empfangen

Ein `KafkaStartEventBean` für Nutzung in einem Efeu *Programmheft Start* Element
ist versehen zu anhören Gegenstand Muster und Start #Ivy verarbeitet. Wähl aus
herein diese Bohne die *Start* Deckel von einen *Programmheft Start* Element:

![KafkaStartEventBean](images/KafkaStartEventBeanStart.png)

Konfigurier herein einige zuzüglichen #Besitz die *Chefredakteur* Deckel von den
*Programmheft Start* Element:

![KafkaStartEventBean](images/KafkaStartEventBeanEditor.png)

**Gegenstand Muster** Betritt ein gültiges `java.util.regex.Muster` für den
Gegenstand(s) zu anhören. Note, jene Wörter ohne speziell chararcters sind
gültige Muster. So dort ist keine Notwendigkeit zu lernen eine spezielle Syntax
zu anhören simple Gegenstand Namen. Note, jene Gegenstand Muster sind Fall
sensibel.

**Synchron** #Wann ist empfangen eine Meldung, will die Bohne Wartezeit bis die
gestartet Arbeitsgang gibt zurück Aufsicht (synchron) oder fortdauern zu
empfangen #parallel Meldungen (asynchron)? Alle asynchronen Bohnen teilen ein
lediges Garn Kader und die Größe von diesem Kader ist konfiguriert #global.
Synchrone Bohnen wollen ihr eigenes Garn benutzen. In der #voreingestellt
Konfiguration, Meldungen sind commited automatisch. Ob du möchtest Meldungen
begehen #du, du darfst mögen schalten zu synchron Verfahren und benutzen das
beliefert Konsumenten zu begehen die Meldung Verschiebung. Mögliche Werte sind
`wahr` oder `falsch`. #Alles #welche #auswerten nicht zu `wahr` (in #Java
`Aussagenlogisch.valueOf(#Aufreihen)`) wollen sein nachgedacht `falsch` #welche
ist auch Vorgabe (asynchrones Meldung Handing).

**Konfiguration Name** Der Name von einer Gruppe von global Variablen unterhalb
diesen Pfad zu benutzen da #Besitz für Satzbau von einen `Konsumenten`.

#### #Zugreifen #der #Daten

Wann eine Meldung ist empfangen, der Arbeitsgang Start will sein gefeuert und
die folgenden Variablen in eure #Daten Klasse will sein gesetzt zu den Werten
empfangen. Note, dass eure #Daten-Klasse muss diese mit dem richtigen Typ
versehen:

**Konsument** dies ist der `Konsument` #welche empfangen die Meldung. Es kann
sein benutzt zu begehen eine Meldung, beispielsweise. Der Typ von der
`Konsument` Feld muss sein `org.apache.kafka.Kunden.Konsument.Konsument`.

**consumerRecord** Dies ist das `ConsumerRecord` empfangen #bei dem Konsumenten.
Es darf zügeln ein `Schlüssel` und einen `Wert` und gibt du greifst zu zu den
`Gegenstand` und `Verschiebung`. Note, dass die `consumerRecord` liefert an
`Schlüssel` und `Wert` da `Objekt` Typ. Ob du konfiguriertest einen Spezial
#Kafka Deserializer du willst müssen entledigen manuell die anerkannten Objekte
zu dem richtigen Typ. Der Typ von der `consumerRecord` Feld muss sein
`org.apache.kafka.Kunden.Konsument.ConsumerRecord`.

#### #Zurückgeben Aufsicht nach bedienen eine Meldung

Wann ein Garn (verarbeite) ist bedienen eine anerkannte Meldung, es will sein
belegt bis die Arbeitsgang Enden oder den Task ist unterbrochen. Es ist gute
Fahrpraxis zu vermeiden blocken #lange einen Arbeitsgang. Ob du brauchst zu tun
komplex, Zeit-konsumierend Operationen, #nachdenken senden ein Signal zu starten
anderen Arbeitsgang zu tun diesen arbeiten

Note, jener `Konsument`s Henkel Meldung queueing, so wollen keine Meldungen sein
gefehlt, sogar ob alle Garne sind zurzeit fleißig. #Sowie ist ein Garn frei, die
nächste Meldung will sein bedient.

### #Senden und empfangend Objekte folgend ein Schema (benutzend das Schema #Standesamt)

Dieser Anschluss auch sendend Unterstützungen und empfangend Objekte definiert
in einem Plan angemeldet an ein Schema #Standesamt Server. Zurzeit, das AVRO
Plan ist unterstützt direkt #bei dem Anschluss. Einrichtung von so kann eine
Konfiguration sein gefunden [auf dem
Internet](https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/serdes-avro.html)
und dem brauchte #Besitz können sein definiert da global Variablen genauso #was
nicht-schematisches #Senden und Empfang.

Zu arbeiten mit Schemata, schafft ein Schema (#z.B. `Person.avsc`) Und gelassen
Experten schafft #der #Java #eingruppieren für jenes Schema. Das `pom.xml` Datei
von das Demo Projekt zeigt ein Beispiel von rennen die Quelle Erzeugung. Quelle
ist generiert mal die Statur starten mit die `generiert-avro` Profil:

```
mvn generate-sources -Pgenerate-avro
```

Ob eure Schema #Standesamt ist gesetzt-bis automatisch akzeptieren ein neues
Schema, du kannst direkt Objekte zu #Kafka senden.

#### Empfangend GenericData Schallplatten

Ob das globales `#Spezifikum.avro.Leser` ist gesetzt zu `falsch`, AVRO will
deserialize Meldungen hinein `GenericData.Schallplatte`s. Dies ist ein typisches
Objekt Opfergabe Annehmlichkeit Methoden zu zugreifen Felder, Bereiche oder
gleichmäßige #Ersatz-Schallplatten sicher. Für simpel Objekte, dieses Konzept
ist Kuhle passte.

#### Empfang #Java wendet ein

Ob das globales `#Spezifikum.avro.Leser` ist gesetzt zu `wahr`, AVRO will
deserialize Meldungen hinein Objekte geschafft bei die Experten Stufe beschrieb
eher. Dort ist ein Vorbehalt doch. Die Klassen zurückgekehrt müssen sein herein
verfügbar die classpath, #wann ist empfangen eine Meldung.

#Technisch, ob eine Klasse ist benutzt erstmals in einem Ersatz-Arbeitsgang von
dem Anschluss (#d.h. für senden) und nachher möchtest du deserialize hinein
diese Klasse, es will nicht sein gefunden nochmal. Dies ist punkto den #Kafka
caching Mechanismus und Efeu #Spezifikum classloader bedienen von Projekte.

Da eine Lösung, ob du erfährst das Problem für #man von euren Klassen, Schalter
zu #der Annehmlichkeit #Java holt ab senden oder schaffen einen
Ersatz-Arbeitsgang in eurem eigenen Projekt. Ein Beispiel für senden und
empfangend Objekte sind gezeigt in das Demo Projekt.

### Kreation von Produzent und Konsument

In einige Komplex Umwelten (und so auch in #Ivy) #Kafka ist manchmal nicht
#imstande zu zugreifen die richtige Klasse #Lader zu schaffen `Konsumenten`s und
`Produzenten`s. Dieser Anschluss versieht Annehmlichkeit Aufgaben
`Konsumenten()` und `Produzenten()` zu arbeiten herein um dieses Problem das
`KafkaService` #eingruppieren und kann auch sein benutzt zu schaffen #Spezifikum
`Konsumenten`s und `Produzenten`s nicht #unter Kontrolle #bei dem Anschluss
caching Mechanismus direkt via einen Apparat von #Besitz.

Außerdem das `KafkaStartEventBean` akzeptiert den Namen von einen `Konsumenten`
Zulieferer erlaubt jener die Nutzung von `Konsumenten`s geschafft in einigem
verschiedenen Weg.

### Konfiguration

> [!BEACHTE] Den variablen Pfad `kafka-Anschluss` gewechselt zu `kafkaConnector`
> von Version 12.0.2.

Konfiguration kann sein getan in global Variablen #wo #welche simpel inheritence
Mechanismus ist versehen. Alle #Kafka Konfiguration ist gelagert unten die
`kafkaConnector` globale Variable. An diesem Level solltest du konfigurieren die
folgenden globalen Lagen.

**workerPoolSize** Nummer von Arbeiter Garne geteilt #bei alle Konsumenten zu
bedienen #parallel #Kafka Meldungen.

**pollTimeoutMs** Konsument #Umfragen misst herein ms. Note, jene Meldungen sind
immer sofort empfangen. Dieser Zeitüberschreitung Wert definiert das Polle
Intervall. Auch will es sein die #höchster Zeit brauchte zu automatisch
herausfinden Konfiguration Änderungen (Änderung von `configId`).

#### Besitz Blöcke und inheritence

Die Konfiguration Unterstützungen mehrfach-Instanzen. Es zügelt Besitz Blöcke
unterhalb Konfiguration Namen. Beispielsweise, die Lagen zügelten in dem Block
`kafkaConnector.localhost` Wollen sein benutzt, #wann ein prodcuer ist geschafft
mit `KafkaService.Bekomm().createProducer("localhost")`.

Alle Lagen (außer die Lage `erbt`) unterhalb diesen Namen will sein eingesammelt
hinein einen `#Besitz` wenden ein und abgespielt zu dem Erbauer von den #Kafka
Konsumenten oder Produzenten Objekte.

Die spezielle Lage `erbt` kann sein benutzt zu #referenzieren anderen
Konfiguration Block jener kann sein benutzt und overridden. (Inheritence Ist
rekursiv und wollen überprüfen für kränklich Loopings.) Der Anschluss definiert
ein `defaultConfig` Block mit einige Anger Lagen. Es macht gewöhnlich Sinn zu
erben eure Konfiguration von diesem Block. Für ein Beispiel von einer simplen
Konfiguration #welche erbt von der `defaultConfig` Konfiguration, habe eine
ansehen das Demo Projekt!

Die spezielle Lage `configId` ist benutzt zu herausfinden Änderungen in der
Konfiguration. Der effektive Wert gelegt dort bedeutet nicht, es kann eine
simple Nummer oder einiger Text sein oder sogar ein #Zeitstempel. #Wann immer
diese Wert Änderungen, alle Produzenten und Konsumenten angegangen #bei der
Änderung will sein wiedererschafft automatisch die neue Konfiguration zu
wiedergeben. Produzenten wollen reagieren an die #nächste senden, Konsumenten
wollen reagieren #wann ist empfangen eine neue Meldung (bei der alten
Konfiguration) oder automatisch, #wann immer geschieht eine neue Polle (#welche
ist definiert mal `pollTimeoutMs`). Note, dass die `configId` kann sein
inheritted, so wechselnd es für eine ledige Konfiguration will nur Produzenten
und Konsumenten für diese spezifische Konfiguration verbessern #während
verbessernd die `defaultConfig` will alle Produzenten und Konsumenten
verbessern.
