package com.axonivy.connector.kafka.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.connector.kafka.KafkaConfiguration;

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

		var defaultProps = KafkaConfiguration.get("default").getProperties();

		assertThat(defaultProps.getProperty("setting.a")).as("Correct setting of default setting a").isEqualTo("A1");
		assertThat(defaultProps.getProperty("setting.b")).as("Correct setting of default setting a").isEqualTo("B1");

		var myProps = KafkaConfiguration.get("my").getProperties();

		assertThat(myProps.getProperty("setting.a")).as("Correct setting of my setting a").isEqualTo("A2");
		assertThat(myProps.getProperty("setting.b")).as("Correct setting of my setting a").isEqualTo("B1");
		assertThat(myProps.getProperty("inherit")).as("Remove option inherit after handling").isNull();
	}
}
