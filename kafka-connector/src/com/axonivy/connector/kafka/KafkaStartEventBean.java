/**
 * 
 */
package com.axonivy.connector.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import ch.ivyteam.ivy.process.eventstart.IProcessStarter;
import ch.ivyteam.ivy.process.extension.ui.ExtensionUiBuilder;
import ch.ivyteam.ivy.process.extension.ui.IUiFieldEditor;
import ch.ivyteam.ivy.process.extension.ui.UiEditorExtension;
import ch.ivyteam.ivy.request.RequestException;
import ch.ivyteam.ivy.service.ServiceException;
import ch.ivyteam.log.Logger;
import ch.ivyteam.util.CleanProperties;

/**
 * {@link IProcessStartEventBean} to listen to Apache Kafka topics.
 */
public class KafkaStartEventBean extends AbstractProcessStartEventBean {
	private static final String KAFKA_CONFIGURATION_NAME_FIELD = "kafkaConfigurationNameField";
	private static final String TOPIC_PATTERN_FIELD = "topicPatternField";
	private static final String SYNCHRONOUS_FIELD = "synchronousField";
	private Properties beanConfiguration;
	private KafkaReader reader = new KafkaReader();

	public KafkaStartEventBean() {
		super("KafkaStartEventBean", "Listen on Kafka topics");
	}

	@Override
	public void initialize(IProcessStartEventBeanRuntime eventRuntime, String configuration) {
		super.initialize(eventRuntime, configuration);
		log().debug("Initializing with configuration: {0}", getConfiguration());
		eventRuntime.poll().disable();
		eventRuntime.threads().boundToEventLifecycle(reader::run);
		beanConfiguration = new CleanProperties(getConfiguration()).getWrappedProperties();
	}
	

	@Override
	public synchronized void start(IProgressMonitor monitor) throws ServiceException {
		String kafkaConfigurationName = getKafkaConfigurationName();
		var kafkaConfig = KafkaService.get().getConfigurationProperties(kafkaConfigurationName);

		log().debug("Starting Kafka consumer for topic pattern: ''{0}'' and configuration name: ''{1}''",
				getTopicPattern(), kafkaConfigurationName);

		KafkaTopicConsumerSupplier<?, ?> topicConsumerSupplier = new DefaultTopicConsumerSupplier<>();

		String topicConsumerSupplierClass = KafkaService.get().getTopicConsumerSupplier();
		if(topicConsumerSupplierClass != null) {
			try {
				topicConsumerSupplier = (KafkaTopicConsumerSupplier<?, ?>) Class.forName(topicConsumerSupplierClass).getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				log().error("Error while finding topic consumer supplier ''{0}'', falling back to default consumer supplier ''{1}''.",
						topicConsumerSupplierClass, topicConsumerSupplier.getClass().getCanonicalName());
			}
		}
		reader.start(kafkaConfig, topicConsumerSupplier, isSynchronous(), Duration.ofMillis(KafkaService.get().getPollTimeoutMs()));
		super.start(monitor);
		log().info("Started");
	}
	
	@Override
	public synchronized void stop(IProgressMonitor monitor) throws ServiceException {
		reader.stop();
		super.stop(monitor);
	}

	@Override
	public void poll() {
		log().warn("Did not expect call to poll (polling was disabled).");
	}

	protected Logger log() {
		return getEventBeanRuntime().getRuntimeLogLogger();
	}

	protected String getKafkaConfigurationName() {
		return beanConfiguration.getProperty(KAFKA_CONFIGURATION_NAME_FIELD);
	}

	protected String getTopicPattern() {
		return beanConfiguration.getProperty(TOPIC_PATTERN_FIELD);
	}

	protected boolean isSynchronous() {
		String synchronousString = beanConfiguration.getProperty(SYNCHRONOUS_FIELD);
		return StringUtils.isNotBlank(synchronousString) && Boolean.valueOf(synchronousString);
	}
	
	private final class KafkaReader {
		private record AsyncRequest(ConsumerRecord<?,?> record, Future<IProcessStartEventResponse> future) {};
		private ArrayList<AsyncRequest> asyncRequests = new ArrayList<>();
		private Properties configuration;
		private KafkaTopicConsumerSupplier<?, ?> topicConsumerSupplier;
		private boolean synchronous;
		private Duration pollTimeout;
		private KafkaConsumer<?, ?> consumer;

		public synchronized void start(Properties configuration, KafkaTopicConsumerSupplier<?, ?> topicConsumerSupplier, boolean synchronous, Duration pollTimeout) {
			this.configuration = configuration;
			this.topicConsumerSupplier = topicConsumerSupplier;
			this.synchronous = synchronous;
			this.pollTimeout = pollTimeout;
		}
		
		public void stop() {
			if(consumer != null) {
				consumer.wakeup();
			}
		}

		private void run() {
			try {
				try {
					log().debug("Handle Kafka messages synchronously: {0}", synchronous);
					
					createConsumer();

					while(!Thread.currentThread().isInterrupted()) {
						ConsumerRecords<?, ?> records = consumer.poll(pollTimeout);
						for (ConsumerRecord<?, ?> record : records) {
							log().debug("Received record, topic: {0} offset:{1} key: {2} val: {3} record: {4}",
									record.topic(), record.offset(), record.key(), record.value(), record);
							
							startProcess(record, synchronous);
						}
						consumeAsyncRequests();
					}
				} finally {
					closeConsumer();
				}
			} catch (WakeupException | InterruptException e) {
				log().info("Consumer was woken up. This is ok, when there is a shutdown.");
				return;
			}
			catch(Exception e) {
				log().error("Exception while listening for topic, closing consumer.", e);
				throw new RuntimeException("Exception while listening for topic.", e);
			}
		}

		
		private void startProcess(ConsumerRecord<?, ?> record, boolean synchronous) {
			log().debug("Firing task to handle Kafka topic ''{0}'' offset {1}.", record.topic(), record.offset());
			IProcessStarter processStarter = getEventBeanRuntime()
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
		
		private synchronized void createConsumer() {
			consumer = topicConsumerSupplier.apply(configuration, getTopicPattern());
			log().debug("Got consumer {0} which subscribed to Kafka topic pattern: {1}", consumer, getTopicPattern());
		}

		private synchronized void closeConsumer() {
			if(consumer != null) {
				consumer.close();
			}
			consumer = null;
		}

	}

	/**
	 * The editor to configure a {@link KafkaStartEventBean}.
	 */
	public static class Editor extends UiEditorExtension {
		private IUiFieldEditor topicPatternField;
		private IUiFieldEditor synchronousField;
		private IUiFieldEditor kafkaConfigurationNameField;

		@Override
		public void initUiFields(ExtensionUiBuilder ui) {
			ui.label("Topic Pattern:").create();
			topicPatternField = ui.textField().create();

			ui.label("Synchronous:").create();
			synchronousField = ui.textField().create();

			ui.label("Configuration Base:").create();
			kafkaConfigurationNameField = ui.textField().create();

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
					""", KafkaService.get().getKafkaGlobalVariableName());
			ui.label(helpTopic).multiline().create();
		}

		@Override
		protected void loadUiDataFromConfiguration() {
			topicPatternField.setText(getBeanConfigurationProperty(TOPIC_PATTERN_FIELD));
			String synchronous = getBeanConfigurationProperty(SYNCHRONOUS_FIELD);
			if(StringUtils.isBlank(synchronous) || !Boolean.valueOf(synchronous)) {
				Logger.getLogger(KafkaStartEventBean.class).warn("Correcting synchronous value '" + synchronous + "' to false");
				synchronous = "false";
			}
			synchronousField.setText(synchronous);
			kafkaConfigurationNameField.setText(getBeanConfigurationProperty(KAFKA_CONFIGURATION_NAME_FIELD));
		}

		@Override
		protected boolean saveUiDataToConfiguration() {
			// Clear the bean configuration and all its properties to flush outdated configurations.
			clearBeanConfiguration();

			setBeanConfigurationProperty(TOPIC_PATTERN_FIELD, topicPatternField.getText());
			setBeanConfigurationProperty(SYNCHRONOUS_FIELD, synchronousField.getText());
			setBeanConfigurationProperty(KAFKA_CONFIGURATION_NAME_FIELD, kafkaConfigurationNameField.getText());
			return true;
		}
	}
}
