package com.axonivy.connector.kafka;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.utils.Utils;

import ch.ivyteam.ivy.environment.Ivy;
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
	 * Create a {@link KafkaProducer} configured by given properties name.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param properties
	 * @return
	 */
	public <K, V> KafkaProducer<K, V> producer(Properties properties) {
		return executeWithKafkaClassLoader(() -> new KafkaProducer<>(properties));
	}

	/**
	 * Get a {@link KafkaProducer} configured by configuration name.
	 * 
	 * Re-uses cached producer if available.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param configurationName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> KafkaProducer<K, V> producer(String configurationName) {
		var cached = KafkaConfiguration.get(configurationName);
		if(!cached.isProducerValid()) {
			synchronized(this) {
				if(!cached.isProducerValid()) {
					if(cached.getProducer() != null) {
						Ivy.log().info("Closing existing {0} for configuration ''{1}''", KafkaProducer.class.getSimpleName(), configurationName);
						cached.getProducer().close();
						cached.setProducer(null);
					}
					Ivy.log().info("Creating a new {0} for configuration ''{1}''", KafkaProducer.class.getSimpleName(), configurationName);
					cached.setProducer(producer(cached.getProperties()));
					cached.setProducerValid(true);
				}
			}
		}
		return (KafkaProducer<K, V>) cached.getProducer();
	}

	/**
	 * Create a {@link KafkaConsumer} configured by given properties.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param properties
	 * @return
	 */
	public <K, V> KafkaConsumer<K, V> consumer(Properties properties) {
		return executeWithKafkaClassLoader(() -> new KafkaConsumer<>(properties));
	}

	/**
	 * Create a {@link KafkaConsumer} configured by configuration name.
	 * 
	 * Consumers are not thread-safe, so always create a new consumer.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param configurationName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> KafkaConsumer<K, V> consumer(String configurationName) {
		return consumer(KafkaConfiguration.get(configurationName).getProperties());
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
		for (var configurationName : KafkaConfiguration.getConfigurations().keySet()) {
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
