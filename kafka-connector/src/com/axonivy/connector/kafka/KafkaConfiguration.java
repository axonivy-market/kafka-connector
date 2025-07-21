package com.axonivy.connector.kafka;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.data.cache.IDataCache;
import ch.ivyteam.ivy.environment.Ivy;

public class KafkaConfiguration {
	private static final String KAFKA_GLOBAL_VARIABLE = "kafkaConnector";
	private static final String CONFIG_ID_VARIABLE = "configId";
	private static final String CONFIG_CACHE_GROUP = "Kafka Configuration";

	private String name;
	private Properties properties;
	private boolean producerValid = false;
	private KafkaProducer<?, ?> producer;

	public static String getKafkaGlobalVariable() {
		return KAFKA_GLOBAL_VARIABLE;
	}

	/**
	 * Return the configuration properties of a specific Kafka configuration stored in global variables.
	 * 
	 * @param configurationName
	 * @return
	 */
	public static KafkaConfiguration get(String configurationName) {
		var properties = getConfigurationProperties(configurationName);

		var cached = getCachedConfiguration(configurationName);

		if(cached == null || !isEqual(cached.getConfigId(), properties.get(CONFIG_ID_VARIABLE))) {
			synchronized (KafkaConfiguration.class) {
				cached = getCachedConfiguration(configurationName);
				if(cached == null || !isEqual(cached.getProperties().get(CONFIG_ID_VARIABLE), properties.get(CONFIG_ID_VARIABLE))) {
					Ivy.log().info("Creating or updating configuration ''{0}''", configurationName);
					if(cached == null) {
						cached = new KafkaConfiguration();
						setCachedConfiguration(configurationName, cached);
					}
					cached.setName(configurationName);
					cached.setProperties(properties);
					cached.setProducerValid(false);
				}
			}
		}
		return cached;
	}


	public static KafkaConfiguration getCachedConfiguration(String configurationName) {
		var entry = cache().getEntry(CONFIG_CACHE_GROUP, configurationName);
		return entry != null ? (KafkaConfiguration) entry.getValue() : null;
	}


	public static void setCachedConfiguration(String configurationName, KafkaConfiguration configuration) {
		cache().setEntry(CONFIG_CACHE_GROUP, configurationName, configuration);
	}

	protected static IDataCache cache() {
		return IDataCache.of(IApplication.current());
	}

	public static List<String> getAllCachedConfigurations() {
		var group = cache().getGroup(CONFIG_CACHE_GROUP);
		return group.getEntries().stream().map(e -> e.getIdentifier()).toList();
	}

	/**
	 * Get the id of this configuration.
	 * 
	 * The id can be set in the global variables and indicates a change.
	 * 
	 * @return
	 */
	public String getConfigId() {
		var id = properties != null ? properties.get(CONFIG_ID_VARIABLE) : null;
		return id != null ? id.toString() : null;
	}

	/**
	 * Does this configuration still have this configuration id?
	 * 
	 * The id can be set in the global variables and indicates a change.
	 * 
	 * @param configId
	 * @return
	 */
	public boolean hasConfigId(String configId) {
		return isEqual(getConfigId(), configId);
	}

	/**
	 * Does the given configuration have the same configuration id?
	 * 
	 * The id can be set in the global variables and indicates a change.
	 * 
	 * @param configuration
	 * @return
	 */
	public boolean hasSameConfigId(KafkaConfiguration configuration) {
		return hasConfigId(configuration.getConfigId());
	}


	protected static boolean isEqual(Object left, Object right) {
		return (left == null && right == null) || 
				(left != null && right != null && left.toString().equals(right.toString()));
	}

	/**
	 * Get the configuration properties for a specific name and also handle inheritance from super configurations.
	 * 
	 * @param configurationName
	 * @return
	 */
	public static Properties getConfigurationProperties(String configurationName) {
		return mergeConfigurationProperties(configurationName, new HashSet<String>(), new Properties());
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
	 * @return 
	 */
	protected static Properties mergeConfigurationProperties(String configurationName, Set<String> seen, Properties properties) {
		if(!seen.add(configurationName)) {
			throw BpmError.create("kafka:connector:configloop").withMessage("Found configuration loop with already seen configuration '" + configurationName + "'.").build();
		}
		var whichAbs = KAFKA_GLOBAL_VARIABLE + "." + configurationName + ".";

		var inheritedProperties = new Properties();
		var newProperties = new Properties();

		for (var v : Ivy.var().all()) {
			var name = v.name();
			if(name.startsWith(whichAbs)) {
				name = name.substring(whichAbs.length());
				var value = v.value();
				if(name.equals("inherit")) {
					properties = mergeConfigurationProperties(value, seen, inheritedProperties);
				}
				else {
					newProperties.put(name, value);
				}
			}
		}

		properties.putAll(inheritedProperties);
		properties.putAll(newProperties);
		return properties;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Properties getProperties() {
		return properties;
	}
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	public boolean isProducerValid() {
		return producerValid;
	}
	public void setProducerValid(boolean producerValid) {
		this.producerValid = producerValid;
	}
	public KafkaProducer<?, ?> getProducer() {
		return producer;
	}
	public void setProducer(KafkaProducer<?, ?> producer) {
		this.producer = producer;
	}

	/**
	 * Convert {@link Properties} to {@link String}.
	 * 
	 * @param properties
	 * @return
	 */
	@Override
	public String toString() {
		return "Configuration: '%s'%n%s".formatted(
				name,
				properties.entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getKey().toString()))
				.map(e -> "%s: %s".formatted(e.getKey(), e.getValue()))
				.collect(Collectors.joining("\n")));
	}


}