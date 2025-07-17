package com.axonivy.connector.kafka;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.utils.Utils;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.restricted.IvyThreadLocalNameConstants;
import ch.ivyteam.ivy.vars.Variable;
import ch.ivyteam.util.threadcontext.IvyThreadContext;

/**
 * Functions to support working with Apache Kafka.
 */
public class KafkaService {
	private static final String WORKER_POOL_SIZE = "workerPoolSize";
	private static final String POLL_TIMEOUT_MS = "pollTimeoutMs";
	private static final String TOPIC_CONSUMER_SUPPLIER = "topicConsumerSupplier";
	private static KafkaService INSTANCE = new KafkaService();
	private static final String KAFKA_GLOBAL_VARIABLE = "kafka-connector";

	private KafkaService() {
	}

	public String getKafkaGlobalVariableName() {
		return KAFKA_GLOBAL_VARIABLE;
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
	public <K, V> KafkaProducer<K, V> createProducer(Properties properties) {
		return executeWithKafkaClassLoader(() -> new KafkaProducer<>(properties));
	}

	/**
	 * Create a {@link KafkaProducer} configured by configuration name.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param configurationName
	 * @return
	 */
	public <K, V> KafkaProducer<K, V> createProducer(String configurationName) {
		return createProducer(getConfigurationProperties(configurationName));
	}

	/**
	 * Create a {@link KafkaConsumer} configured by given properties.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param properties
	 * @return
	 */
	public <K, V> KafkaConsumer<K, V> createConsumer(Properties properties) {
		return executeWithKafkaClassLoader(() -> new KafkaConsumer<>(properties));
	}

	/**
	 * Create a {@link KafkaConsumer} configured by configuration name.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param configurationName
	 * @return
	 */
	public <K, V> KafkaConsumer<K, V> createConsumer(String configurationName) {
		return createConsumer(getConfigurationProperties(configurationName));
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
		return getVar(WORKER_POOL_SIZE, v -> StringUtils.isNotBlank(v) ? Integer.valueOf(v) : defaultValue, defaultValue);
	}

	/**
	 * Get the configured poll timeout in ms.
	 * 
	 * @return
	 */
	public long getPollTimeoutMs() {
		long defaultValue = 60000L;
		return getVar(POLL_TIMEOUT_MS, v -> StringUtils.isNotBlank(v) ? Long.valueOf(v) : defaultValue, defaultValue);
	}

	/**
	 * Get the configured topic consumer {@link Supplier}.
	 * 
	 * @return
	 */
	public String getTopicConsumerSupplier() {
		return getVar(TOPIC_CONSUMER_SUPPLIER, v -> ( StringUtils.isNotBlank(v) ? v.trim() : null), (String)null);
	}

	/**
	 * Return the configuration properties of a specific Kafka configuration stored in global variables.
	 * 
	 * @param configurationName
	 * @return
	 */
	public Properties getConfigurationProperties(String configurationName) {
		Properties properties = new Properties();
		Set<String> seen = new HashSet<>();

		mergeProperties(configurationName, seen, properties);

		return properties;
	}

	/**
	 * Convert {@link Properties} to {@link String}.
	 * 
	 * @param properties
	 * @return
	 */
	public String getPropertiesString(Properties properties) {
		return properties.entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getKey().toString()))
				.map(e -> String.format("%s: %s", e.getKey(), e.getValue()))
				.collect(Collectors.joining("\n"));
	}

	protected <T> T getVar(String varName, Function<String, T> converter, T defaultValue) {
		T returnValue = defaultValue;
		String value = Ivy.var().get(varName);
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
	 * Read properties from global variables.
	 * 
	 * If a configuration contains a value for inherited,
	 * then the inherited configuration will be read first.
	 * 
	 * @param configurationName
	 * @param seen
	 * @param properties
	 */
	protected void mergeProperties(String configurationName, Set<String> seen, Properties properties) {
		if(!seen.add(configurationName)) {
			throw BpmError.create("kafka:connector:configloop").withMessage("Found configuration loop with already seen configuration '" + configurationName + "'.").build();
		}
		String whichAbs = KAFKA_GLOBAL_VARIABLE + "." + configurationName + ".";

		Properties inheritedProperties = new Properties();
		Properties newProperties = new Properties();

		for (Variable v : Ivy.var().all()) {
			String name = v.name();
			if(name.startsWith(whichAbs)) {
				name = name.substring(whichAbs.length());
				String value = v.value();
				if(name.equals("inherit")) {
					mergeProperties(value, seen, inheritedProperties);
				}
				else {
					newProperties.put(name, value);
				}
			}
		}

		properties.putAll(inheritedProperties);
		properties.putAll(newProperties);
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
