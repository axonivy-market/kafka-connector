package com.axonivy.connector.kafka;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.avro.generic.GenericData;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.utils.Utils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.project.IIvyProject;
import ch.ivyteam.ivy.restricted.IvyThreadLocalNameConstants;
import ch.ivyteam.util.threadcontext.IvyThreadContext;

/**
 * Functions to support working with Apache Kafka.
 */
public class KafkaService {
	private static final String WORKER_POOL_SIZE = "workerPoolSize";
	private static final String POLL_TIMEOUT_MS = "pollTimeoutMs";
	private static final String TOPIC_CONSUMER_SUPPLIER = "topicConsumerSupplier";
	private static KafkaService INSTANCE = new KafkaService();

	private KafkaService() {
	}

	/**
	 * Get instance of service.
	 * 
	 * @return
	 */
	public static KafkaService get() {
		return INSTANCE;
	}

	/**
	 * Get a {@link Producer} configured by configuration name.
	 * 
	 * Re-uses cached producer if available.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param configurationName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Producer<K, V> producer(String configurationName) {
		var cached = KafkaConfiguration.get(configurationName);
		if(!cached.isProducerValid()) {
			synchronized(this) {
				if(!cached.isProducerValid()) {
					if(cached.getProducer() != null) {
						Ivy.log().info("Closing existing {0} for configuration ''{1}''", ProjectAwareKafkaProducer.class.getSimpleName(), configurationName);
						cached.getProducer().close();
						cached.setProducer(null);
					}
					Ivy.log().info("Creating a new {0} for configuration ''{1}''", ProjectAwareKafkaProducer.class.getSimpleName(), configurationName);
					cached.setProducer(producer(cached.getProperties()));
					cached.setProducerValid(true);
				}
			}
		}
		return (Producer<K, V>) cached.getProducer();
	}

	/**
	 * Create a {@link Consumer} configured by configuration name.
	 * 
	 * Consumers are not thread-safe, so always create a new consumer.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param configurationName
	 * @return
	 */
	public <K, V> Consumer<K, V> consumer(String configurationName) {
		return consumer(KafkaConfiguration.get(configurationName).getProperties());
	}

	/**
	 * Convenience method to send messages from Java code.
	 * 
	 * <p>
	 * Sending from Java code is recommended, when a Schema Registry is used and objects
	 * should be sent and received (serialized and deserialized automatically), i.e.
	 * you have set the variable </code>specific.avro.deserializing: true</code>.
	 * Sending cannot be done with the connector sub-process in this case because
	 * it does not have access to classes of your project and deserialization will fail
	 * with a "class not found" error.
	 * </p>
	 * 
	 * <p>
	 * Note, that you can still use the connector sub-process for sending if you set
	 * </code>specific.avro.deserializing: false</code>. In this case, objects received
	 * will not be deserialized directly into your objects, but rather a {@link GenericData.Record}
	 * will be returned. This might be enough, if only a few attributes of the object
	 * are needed.
	 * </p>
	 * 
	 * @param <K>
	 * @param <V>
	 * @param configurationName
	 * @param topic
	 * @param key
	 * @param value
	 * @param callback can be <code>null</code>
	 * @return
	 */
	public <K, V> Future<RecordMetadata> send(String configurationName, String topic, K key, V value, Callback callback) {
		return producer(configurationName).send(new ProducerRecord<>(topic, key, value), callback);
	}

	/**
	 * Create a {@link Producer} configured by given properties name.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param properties
	 * @return
	 */
	public <K, V> Producer<K, V> producer(Properties properties) {
		Ivy.log().debug("Creating producer with properties: {0}", properties);
		var producer = executeWithKafkaClassLoader(() -> new KafkaProducer<K, V>(properties));
		return new ProjectAwareKafkaProducer<K, V>(producer);
	}

	/**
	 * Create a {@link Consumer} configured by given properties.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param properties
	 * @return
	 */
	public <K, V> Consumer<K, V> consumer(Properties properties) {
		Ivy.log().debug("Creating consumer with properties: {0}", properties);
		var consumer = executeWithKafkaClassLoader(() -> new KafkaConsumer<K, V>(properties));
		return new ProjectAwareKafkaConsumer<K, V>(consumer);
	}

	/**
	 * Close a producer.
	 * 
	 * @param configurationName
	 */
	public synchronized void closeProducer(String configurationName) {
		var cached = KafkaConfiguration.get(configurationName);
		if(cached.isProducerValid()) {
			cached.getProducer().close();
			cached.setProducer(null);
			cached.setProducerValid(false);
		}
	}

	public void closeAllProducers() {
		for (var configurationName : KafkaConfiguration.getAllCachedConfigurations()) {
			try {
				closeProducer(configurationName);
			} catch (Exception e) {
				Ivy.log().error("Exception closing producer {0}", configurationName);
			}
		}
	}

	/**
	 * Execute a function with a different {@link ClassLoader}.
	 * 
	 * @param <T>
	 * @param classLoader
	 * @param supplier
	 * @return
	 */
	public <T> T executeWithClassLoader(ClassLoader classLoader, Supplier<T> supplier) {
		Thread thread = Thread.currentThread();
		ClassLoader original = thread.getContextClassLoader();
		thread.setContextClassLoader(classLoader);
		T result;
		try {
			result = supplier.get();
		} finally {
			thread.setContextClassLoader(original);
		}
		return result;
	}

	/**
	 * Execute a function with the Kafka {@link ClassLoader}.
	 * 
	 * @param <T>
	 * @param supplier
	 * @return
	 */
	public <T> T executeWithKafkaClassLoader(Supplier<T> supplier) {
		return executeWithClassLoader(Utils.getKafkaClassLoader(), supplier);
	}

	/**
	 * Execute a function with the current project's (i.e. the project where this request was started) {@link ClassLoader}.
	 * 
	 * @param <T>
	 * @param supplier
	 * @return
	 */
	public <T> T executeWithProjectClassLoader(Supplier<T> supplier) {
		return executeWithClassLoader(
				IIvyProject.of(IProcessModelVersion.current())
				.getProjectClassLoader(), supplier);
	}

	/**
	 * Get the configured worker pool size.
	 * 
	 * @return
	 */
	public int getWorkerPoolSize() {
		int defaultValue = 10;
		return getKafkaVar(WORKER_POOL_SIZE, v -> StringUtils.isNotBlank(v) ? Integer.valueOf(v) : defaultValue, defaultValue);
	}

	/**
	 * Get the configured poll timeout in ms.
	 * 
	 * @return
	 */
	public long getPollTimeoutMs() {
		long defaultValue = 60000L;
		return getKafkaVar(POLL_TIMEOUT_MS, v -> StringUtils.isNotBlank(v) ? Long.valueOf(v) : defaultValue, defaultValue);
	}

	/**
	 * Get the configured topic consumer {@link Supplier}.
	 * 
	 * @return
	 */
	public String getTopicConsumerSupplier() {
		return getKafkaVar(TOPIC_CONSUMER_SUPPLIER, v -> ( StringUtils.isNotBlank(v) ? v.trim() : null), (String)null);
	}

	protected <T> T getKafkaVar(String varName, Function<String, T> converter, T defaultValue) {
		T returnValue = defaultValue;
		var value = Ivy.var().get("%s.%s".formatted(KafkaConfiguration.getKafkaGlobalVariable(), varName));
		try {
			returnValue = converter.apply(value);
		}
		catch(Exception e) {
			Ivy.log().error("Could not convert global variable ''{0}'' value ''{1}'', using default ''{2}''",
					e, varName, value, defaultValue);
		}

		return returnValue;
	}


	/**
	 * Create a callback for use within most parts of the Ivy environment.
	 * 
	 * <p>
	 * Note, that some Ivy objects might not be valid in the callback (e.g. request and response).
	 * <h2>Examples</h2>
	 * </p>
	 * 
	 * <code>
	 * <pre>
	 * producer.send(record, ivyCallback(new MyIvyCallback());
	 * </pre>
	 * </code>
	 * or
	 * <code>
	 * <pre>
	 * producer.send(record, ivyCallback((metadata, exception) -> Ivy.wf().signals().send("kafka:demo:signal")));
	 * </pre>
	 * </code>
	 * 
	 * @param callback
	 * @return
	 */
	public IvyCallback ivyCallback(Callback callback) {
		return new IvyCallback(callback);
	}

	private class IvyCallback implements Callback {
		private Object memento;
		private Callback callback;

		@SuppressWarnings("restriction")
		protected IvyCallback(Callback callback) {
			// Save most of the Ivy context but not the servlet request. It will be invalid and every access will throw an
			// IllegalStateException, e.g. Ivy.log() which adds data from the servlet request to the log context.
			this.memento = IvyThreadContext.saveToMemento(List.of(IvyThreadLocalNameConstants.SERVLET_REQUEST));
			this.callback = callback;
		}

		@Override
		public void onCompletion(RecordMetadata metadata, Exception exception) {
			try {
				IvyThreadContext.restoreFromMemento(memento);
				callback.onCompletion(metadata, exception);
			}
			finally {
				IvyThreadContext.remove();
			}
		}
	}
}
