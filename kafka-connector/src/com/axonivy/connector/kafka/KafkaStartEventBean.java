/**
 * 
 */
package com.axonivy.connector.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.WakeupException;
import org.eclipse.core.runtime.IProgressMonitor;

import ch.ivyteam.ivy.process.eventstart.AbstractProcessStartEventBean;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventBean;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventBeanRuntime;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventResponse;
import ch.ivyteam.ivy.process.extension.ProgramConfig;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.request.RequestException;
import ch.ivyteam.ivy.service.ServiceException;
import ch.ivyteam.log.Logger;

/**
 * {@link IProcessStartEventBean} to listen to Apache Kafka topics.
 */
public class KafkaStartEventBean extends AbstractProcessStartEventBean {
	private static final String KAFKA_CONFIGURATION_NAME_FIELD = "kafkaConfigurationNameField";
	private static final String TOPIC_PATTERN_FIELD = "topicPatternField";
	private static final String SYNCHRONOUS_FIELD = "synchronousField";
	private KafkaReader reader = new KafkaReader();

	public KafkaStartEventBean() {
		super("KafkaStartEventBean", "Listen on Kafka topics");
	}

	@Override
	public void initialize(IProcessStartEventBeanRuntime eventRuntime, ProgramConfig configuration) {
		super.initialize(eventRuntime, configuration);
		eventRuntime.poll().disable();
		eventRuntime.threads().boundToEventLifecycle(reader::run);
		log().debug("Initialized KafkaStartEventBean.");
	}

	@Override
	public synchronized void start(IProgressMonitor monitor) throws ServiceException {
		var configurationName = getKafkaConfigurationName();

		log().debug("Starting Kafka consumer for topic pattern: ''{0}'' and configuration name: ''{1}''",
				getTopicPattern(), configurationName);

		KafkaConsumerSupplier<?, ?> topicConsumerSupplier = new DefaultConsumerSupplier<>();

		String topicConsumerSupplierClass = KafkaService.get().getTopicConsumerSupplier();
		if(topicConsumerSupplierClass != null) {
			try {
				topicConsumerSupplier = (KafkaConsumerSupplier<?, ?>) Class.forName(topicConsumerSupplierClass).getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				log().error("Error while finding topic consumer supplier ''{0}'', falling back to default consumer supplier ''{1}''.",
						topicConsumerSupplierClass, topicConsumerSupplier.getClass().getCanonicalName());
			}
		}
		reader.start(configurationName, topicConsumerSupplier, isSynchronous());
		super.start(monitor);
		log().info("Started KafkaStartEventBean.");
	}

	@Override
	public synchronized void stop(IProgressMonitor monitor) throws ServiceException {
		reader.stop();
		super.stop(monitor);
		log().info("Stopped KafkaStartEventBean.");
	}

	@Override
	public void poll() {
		log().warn("Did not expect call to poll (polling was disabled in KafkaStartEventBean).");
	}

	protected Logger log() {
		return getEventBeanRuntime().getRuntimeLogLogger();
	}

	protected String getKafkaConfigurationName() {
		return getConfig().get(KAFKA_CONFIGURATION_NAME_FIELD);
	}

	protected String getTopicPattern() {
		return getConfig().get(TOPIC_PATTERN_FIELD);
	}

	protected boolean isSynchronous() {
		String synchronousString = getConfig().get(SYNCHRONOUS_FIELD);
		return StringUtils.isNotBlank(synchronousString) && Boolean.valueOf(synchronousString);
	}

	private final class KafkaReader {
		private record AsyncRequest(ConsumerRecord<?,?> record, Future<IProcessStartEventResponse> future) {};
		private ArrayList<AsyncRequest> asyncRequests = new ArrayList<>();
		private String configurationName;
		private KafkaConsumerSupplier<?, ?> consumerSupplier;
		private boolean synchronous;
		private KafkaConsumer<?, ?> consumer;

		public synchronized void start(String configurationName, KafkaConsumerSupplier<?, ?> consumerSupplier, boolean synchronous) {
			log().info("KafkaReader start");
			this.configurationName = configurationName;
			this.consumerSupplier = consumerSupplier;
			this.synchronous = synchronous;
		}

		public void stop() {
			log().info("KafkaReader stop");
			if(consumer != null) {
				consumer.wakeup();
			}
		}

		private void run() {
			while(!Thread.currentThread().isInterrupted()) {
				try {
					log().info("Creating consumer for configuration ''{0}'' topic ''{1}'' to handle Kafka messages synchronously: {2}", configurationName, getTopicPattern(), synchronous);

					var lastConfigId = KafkaConfiguration.get(configurationName).getConfigId();

					consumer = consumerSupplier.supply(configurationName);
					consumer.subscribe(Pattern.compile(getTopicPattern()));

					log().debug("Consumer {0} subscribed to Kafka topic pattern: {1}", consumer, getTopicPattern());

					while(!Thread.currentThread().isInterrupted()) {
						log().debug("Polling ''{0}:{1}''", configurationName, getTopicPattern());
						var configuration = KafkaConfiguration.get(configurationName);
						var configId = configuration.getConfigId();
						if(!configuration.hasConfigId(lastConfigId)) {
							log().info("Configuration change detected for consumer {0}:{1}, config Id changed from ''{2}'' to ''{3}''", configurationName, getTopicPattern(), lastConfigId, configId);
							consumer.close();
							log().info("Closed consumer {0}:{1}", configurationName, getTopicPattern());
							consumer = consumerSupplier.supply(configurationName);
							consumer.subscribe(Pattern.compile(getTopicPattern()));
							log().info("Created a new consumer {0}:{1}", configurationName, getTopicPattern());

							lastConfigId = configId;
						}
						var pollTimeout = Duration.ofMillis(KafkaService.get().getPollTimeoutMs());
						ConsumerRecords<?, ?> records = consumer.poll(pollTimeout);
						for (ConsumerRecord<?, ?> record : records) {
							log().debug("Received record, topic: {0} offset:{1} key: {2} val: {3} record: {4}",
									record.topic(), record.offset(), record.key(), record.value(), record);

							startProcess(record, synchronous);
						}
						consumeAsyncRequests();
					}
				}
				catch (WakeupException | InterruptException e) {
					log().info("Consumer was woken up. This is ok, when there is a shutdown. Consumer: {0}", consumer);
				}
				catch(Exception e) {
					log().error("Exception while listening for topic, closing consumer {0}.", e, consumer);
					throw new RuntimeException("Exception while listening for topic.", e);
				}
				finally {
					log().info("Closing consumer {0}", consumer);
					try {
						consumer.close();
					} catch (InterruptException e) {
						log().info("Ignoring exception while closing consumer for configuration ''{0}:{1}'' ({2})", configurationName, getTopicPattern(), e);
					}
					consumer = null;
				}
			}
		}

		private void startProcess(ConsumerRecord<?, ?> record, boolean synchronous) {
			log().debug("Firing task to handle Kafka topic ''{0}'' offset {1}.", record.topic(), record.offset());
			var processStarter = getEventBeanRuntime()
					.processStarter()
					.withReason("Received Kafka record " + record)
					.withParameter("consumer", consumer)
					.withParameter("consumerRecord", record);
			if (synchronous) {
				try {
					var eventResponse = processStarter.start();
					logResponse(record, eventResponse);
				} catch (RequestException e) {
					logError(record, e);
				}
			} else {
				asyncRequests.add(new AsyncRequest(record, processStarter.startAsync()));
			}
		}

		private void logError(ConsumerRecord<?, ?> record, Exception e) {
			log().error("Kafka topic ''{0}'', offset {1} caused exception while firing a new task.", e, record.topic(), record.offset());
		}

		private void logResponse(ConsumerRecord<?, ?> record, IProcessStartEventResponse eventResponse) {
			log().debug("Kafka topic ''{0}'', offset {1} was handled by task {2} and returned with parameters: {3}.",
					record.topic(), record.offset(), eventResponse.getStartedTask().getId(),
					eventResponse.getParameters().keySet().stream().sorted().collect(Collectors.joining(", ")));
		}

		private void consumeAsyncRequests() {
			var consumed = new ArrayList<AsyncRequest>();
			for (var asyncRequest : asyncRequests) {
				if (asyncRequest.future.isDone()) {
					consumed.add(asyncRequest);
					try {
						var eventResponse = asyncRequest.future.get();
						logResponse(asyncRequest.record, eventResponse);
					} catch(ExecutionException ex) {
						logError(asyncRequest.record, ex);
					}
					catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
			asyncRequests.removeAll(consumed);
		}
	}

	/**
	 * The editor to configure a {@link KafkaStartEventBean}.
	 */
	public static class Editor extends UiEditorExtension {

		@Override
		public void initUiFields(ExtensionUiBuilder ui) {
			ui.label("Topic Pattern:").create();
			ui.textField(TOPIC_PATTERN_FIELD).create();

			ui.label("Synchronous:").create();
			ui.textField(SYNCHRONOUS_FIELD).create();

			ui.label("Configuration Base:").create();
			ui.textField(KAFKA_CONFIGURATION_NAME_FIELD).create();

			String helpTopic = String.format("""
					Topic pattern:
					A java.util.regex.Pattern which will be
					used to match topics which will be received.
					For syntax please look into Java API documentation.

					Examples:
					MyTopic-1
					MyTopic-[0-9]+
					(MyTopic|YourTopic)

					Synchronous:
					Flag which determines, if messages will be handled
					synchronously. If true, then only a single thread
					will be used to handle a message. This is useful,
					if the message should not be committed automatically
					but by the started process.

					Default: false

					Configuration name:
					Name of a collection of global variables below
					%s which defines a specific Kafka consumer configuration.
					""", KafkaConfiguration.getKafkaGlobalVariable());
			ui.label(helpTopic).multiline().create();
		}
	}
}
