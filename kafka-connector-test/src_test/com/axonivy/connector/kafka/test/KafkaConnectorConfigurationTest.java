package com.axonivy.connector.kafka.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.axonivy.connector.kafka.KafkaService;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class KafkaConnectorConfigurationTest {

	@Test
	public void testInheritence(AppFixture fixture){
		fixture.var("kafkaConnector.workerPoolSize", "10");
		fixture.var("kafkaConnector.default.setting.a", "A1");
		fixture.var("kafkaConnector.default.setting.b", "B1");
		fixture.var("kafkaConnector.my.inherit", "default");
		fixture.var("kafkaConnector.my.setting.a", "A2");

		Properties defaultProps = KafkaService.get().getConfigurationProperties("default");

		assertThat(defaultProps.get("setting.a")).as("Correct setting of default setting a").isEqualTo("A1");
		assertThat(defaultProps.get("setting.b")).as("Correct setting of default setting a").isEqualTo("B1");

		Properties myProps = KafkaService.get().getConfigurationProperties("my");

		assertThat(myProps.get("setting.a")).as("Correct setting of my setting a").isEqualTo("A2");
		assertThat(myProps.get("setting.b")).as("Correct setting of my setting a").isEqualTo("B1");
		assertThat(myProps.get("inherit")).as("Remove option inherit after handling").isNull();
	}
}
