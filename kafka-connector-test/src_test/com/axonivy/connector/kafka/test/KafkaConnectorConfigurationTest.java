package com.axonivy.connector.kafka.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.axonivy.connector.kafka.KafkaConfiguration;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class KafkaConnectorConfigurationTest {

	@Test
	public void testInheritence(AppFixture fixture){
		fixture.var("kafka-connector.workerPoolSize", "10");
		fixture.var("kafka-connector.default.setting.a", "A1");
		fixture.var("kafka-connector.default.setting.b", "B1");
		fixture.var("kafka-connector.my.inherit", "default");
		fixture.var("kafka-connector.my.setting.a", "A2");

		Properties defaultProps = KafkaConfiguration.get("default").getProperties();

		assertThat(defaultProps.get("setting.a")).as("Correct setting of default setting a").isEqualTo("A1");
		assertThat(defaultProps.get("setting.b")).as("Correct setting of default setting a").isEqualTo("B1");

		Properties myProps = KafkaConfiguration.get("my").getProperties();

		assertThat(myProps.get("setting.a")).as("Correct setting of my setting a").isEqualTo("A2");
		assertThat(myProps.get("setting.b")).as("Correct setting of my setting a").isEqualTo("B1");
		assertThat(myProps.get("inherit")).as("Remove option inherit after handling").isNull();
	}
}
