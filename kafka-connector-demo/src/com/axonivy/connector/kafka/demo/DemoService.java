package com.axonivy.connector.kafka.demo;

import java.util.Properties;

import com.axonivy.connector.kafka.KafkaService;

public class DemoService {
	public static String getConfigurationString(String configurationName) {
		KafkaService kafkaService = KafkaService.get();
		Properties configurationProperties = kafkaService.getConfigurationProperties(configurationName);
		return kafkaService.getPropertiesString(configurationProperties);
	}
}
