/**
 * 
 */
package com.axonivy.connector.kafka;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.eclipse.core.runtime.IProgressMonitor;

import ch.ivyteam.ivy.process.eventstart.AbstractProcessStartEventBean;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventBean;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventBeanRuntime;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventResponse;
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
	private static final AtomicInteger consumerBeanCounter = new AtomicInteger(0);
	private static final String KAFKA_CONFIGURATION_NAME_FIELD = "kafkaConfigurationNameField";
	private static final String TOPIC_PATTERN_FIELD = "topicPatternField";
	private static final String SYNCHRONOUS_FIELD = "synchronousField";
	private static ExecutorService workerExecutor;
	private ExecutorService consumerExecutor;
	private KafkaConsumerRunnable consumerThread;
	private Properties beanConfiguration;
	private Properties varConfiguration;

	public KafkaStartEventBean() {
		super("KafkaStartEventBean", "Listen on Kafka topics");
	}

	@Override
	public void initialize(IProcessStartEventBeanRuntime eventRuntime, String configuration) {
		super.initialize(eventRuntime, configuration);
		log().debug("Initializing with configuration: {0}", getConfiguration());
		eventRuntime.setPollTimeInterval(0);
		beanConfiguration = new CleanProperties(getConfiguration()).getWrappedProperties();
	}

	@Override
	public void start(IProgressMonitor monitor) throws ServiceException {
		String kafkaConfigurationName = getKafkaConfigurationName();
		varConfiguration = KafkaService.get().getConfigurationProperties(kafkaConfigurationName);

		// If any of the start beans is not synchronous, the worker pool is initialized.
		synchronized (KafkaStartEventBean.class) {
			if(!isSynchronous() && workerExecutor == null) {
				int workerPoolSize = KafkaService.get().getWorkerPoolSize();
				// TODO describe that all listeners are shared
				log().info("Found (at least) one asynchronous topic consumer, creating {0} Kafka worker threads.", workerPoolSize);
				workerExecutor = Executors.newFixedThreadPool(workerPoolSize, new NamingThreadFactory("kafka-worker-%d"));
			}
		}

		log().debug("Starting Kafka consumer for topic pattern: ''{0}'' and configuration name: ''{1}''",
				getTopicPattern(), kafkaConfigurationName);

		// TODO describe consumer supplier in doc
		KafkaTopicConsumerSupplier<Object, Object> topicConsumerSupplier = new DefaultTopicConsumerSupplier<>();

		String topicConsumerSupplierClass = KafkaService.get().getTopicConsumerSupplier();
		if(topicConsumerSupplierClass != null) {
			try {
				topicConsumerSupplier = (KafkaTopicConsumerSupplier<Object, Object>) Class.forName(topicConsumerSupplierClass).getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				log().error("Error while finding topic consumer supplier ''{0}'', falling back to default consumer supplier ''{1}''.",
						topicConsumerSupplierClass, topicConsumerSupplier.getClass().getCanonicalName());
			}
		}


		consumerThread = new KafkaConsumerRunnable(varConfiguration, topicConsumerSupplier, Duration.ofMillis(KafkaService.get().getPollTimeoutMs()));
		consumerExecutor = Executors.newSingleThreadExecutor(new NamingThreadFactory("kafka-consumer-" + consumerBeanCounter.incrementAndGet()));
		consumerExecutor.execute(consumerThread);

		super.start(monitor);
		log().info("Started");
	}

	@Override
	public void stop(IProgressMonitor monitor) throws ServiceException {
		if(consumerExecutor != null) {
			consumerExecutor.shutdown();
			consumerThread.wakeup();
			try {
				consumerExecutor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				log().error("Waiting was interrupted", e);
			}
		}

		synchronized (KafkaStartEventBean.class) {
			if(workerExecutor != null) {
				log().info("Shutting down Kafka worker threads");
				workerExecutor.shutdown();
				try {
					workerExecutor.awaitTermination(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					log().error("Waiting was interrupted", e);
				}
				workerExecutor = null;
			}
		}

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

	protected class KafkaWorkerRunnable implements Runnable {
		private ExecutorService workerExecutor;
		private ConsumerRecord<Object, Object> record;
		private KafkaConsumer<Object, Object> consumer;

		protected KafkaWorkerRunnable(ExecutorService executorService) {
			this.workerExecutor = executorService;
		}

		public KafkaWorkerRunnable(ExecutorService workerExecutor, ConsumerRecord<Object, Object> record, KafkaConsumer<Object, Object> consumer) {
			this.workerExecutor = workerExecutor;
			this.record = record;
			this.consumer = consumer;
		}

		@Override
		public void run() {
			if(!workerExecutor.isShutdown()) {
				try {
					log().debug("Firing task to handle Kafka topic ''{0}'' offset {1}.", record.topic(), record.offset());
					IProcessStartEventResponse eventResponse = getEventBeanRuntime().fireProcessStartEventRequest(null, "Received Kafka record " + record, Map.of("consumer", consumer, "consumerRecord", record));
					log().debug("Kafka topic ''{0}'', offset {1} was handled by task {2} and returned with parameters: {3}.",
							record.topic(), record.offset(), eventResponse.getStartedTask().getId(),
							eventResponse.getParameters().keySet().stream().sorted().collect(Collectors.joining(", ")));
				} catch (RequestException e) {
					log().error("Kafka topic ''{0}'', offset {1} caused exception while firing a new task.", e, record.topic(), record.offset());
				}
			}
		}

		/**
		 * Try to wake up so that checking for shutdown is done earlier.
		 */
		public void wakeup() {
			// NOOP
		}
	}

	protected class KafkaConsumerRunnable implements Runnable {
		// TODO mention in doc, that we support Object. Nevertheless, the objects will be converted by the configured Deserializer and can be casted.
		private KafkaConsumer<Object, Object> consumer = null;
		private boolean synchronous = false;
		private Properties properties;
		private KafkaTopicConsumerSupplier<Object, Object> topicConsumerSupplier;
		private Duration pollTimeout;

		private KafkaConsumerRunnable(Properties properties, KafkaTopicConsumerSupplier<Object, Object> topicConsumerSupplier, Duration pollTimeout) {
			this.properties = properties;
			this.topicConsumerSupplier = topicConsumerSupplier;
			this.pollTimeout = pollTimeout;
		}

		@Override
		public void run() {
			try {
				synchronous = isSynchronous();
				log().debug("Handle Kafka messages synchronously: {0}", synchronous);

				consumer = topicConsumerSupplier.apply(properties, getTopicPattern());
				log().debug("Got consumer {0} which subscribed to Kafka topic pattern: {1}", consumer, getTopicPattern());

				while(!consumerExecutor.isShutdown()) {
					try {
						ConsumerRecords<Object, Object> records = consumer.poll(pollTimeout);
						for (ConsumerRecord<Object, Object> record : records) {
							log().debug("Received record, topic: {0} offset:{1} key: {2} val: {3} record: {4}",
									record.topic(), record.offset(), record.key(), record.value(), record);
							KafkaWorkerRunnable kafkaWorkerRunnable = new KafkaWorkerRunnable(workerExecutor, record, consumer);
							if(synchronous) {
								kafkaWorkerRunnable.run();
							}
							else {
								workerExecutor.execute(kafkaWorkerRunnable);
							}
							// TODO describe in doc, that if enable.auto.commit is set to false, then commit must be done in the process (via the consumer object). Note, that commit is only possible for an offset and means that all lower offsets are commited.  
						}
					} catch (WakeupException e) {
						log().info("Consumer was woken up. This is ok, when there is a shutdown.");
					}
				}
			}
			catch(Exception e) {
				log().error("Exception while listening for topic, closing consumer.", e);
			}
			finally {
				if(consumer != null) {
					consumer.close();
				}
			}
		}

		public void wakeup() {
			if(consumer != null) {
				consumer.wakeup();
			}
		}
	}

	/**
	 * A {@link ThreadFactory} which gives threads a configurable name.
	 */
	protected static class NamingThreadFactory implements ThreadFactory {

		private String pattern;
		private static ThreadFactory defaultThreadFactory;
		private static AtomicInteger threadCount = new AtomicInteger(0);

		protected NamingThreadFactory(String pattern) {
			defaultThreadFactory = Executors.defaultThreadFactory();
			this.pattern = pattern;
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = defaultThreadFactory.newThread(runnable);
			thread.setName(String.format(pattern, threadCount.incrementAndGet()));
			return thread;
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
