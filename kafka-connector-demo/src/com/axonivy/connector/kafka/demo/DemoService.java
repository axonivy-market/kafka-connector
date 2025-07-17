package com.axonivy.connector.kafka.demo;

import com.axonivy.connector.kafka.KafkaService;

public class DemoService {
	private static final DemoService INSTANCE = new DemoService();

	public static DemoService get() {
		return INSTANCE;
	}

	public String getConfigurationString(String configurationName) {
		var kafkaService = KafkaService.get();
		var configurationProperties = kafkaService.getConfigurationProperties(configurationName);
		return kafkaService.getPropertiesString(configurationProperties);
	}
}
