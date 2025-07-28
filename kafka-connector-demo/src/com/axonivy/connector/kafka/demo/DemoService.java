package com.axonivy.connector.kafka.demo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.avro.generic.GenericData.Record;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.axonivy.connector.kafka.KafkaService;

import ch.ivyteam.ivy.environment.Ivy;

public class DemoService {
	private static final DemoService INSTANCE = new DemoService();
	private static final Random RND = new Random();
	private static long personId;

	public static DemoService get() {
		return INSTANCE;
	}

	public Person createRandomPerson() {
		return new Person(
				++personId,
				StringUtils.capitalize(RandomStringUtils.randomAlphabetic(5, 10).toLowerCase()),
				StringUtils.capitalize(RandomStringUtils.randomAlphabetic(5, 15).toLowerCase()),
				LocalDate.now().minusDays(365 * (10 + RND.nextInt(90))));
	}

	/**
	 * A single receive just for demo.
	 * 
	 * @return
	 */
	public Person singleReceive() {
		Person person = null;
		try (var consumer = KafkaService.get().consumer("localhostSchematic")) {
			consumer.subscribe(Pattern.compile("PersonTopic"));

			var records = consumer.poll(Duration.ofSeconds(2));

			for (ConsumerRecord<?, ?> r : records) {
				Ivy.log().debug("Received record, topic: {0} offset:{1} key: {2} val: {3} record: {4}",
						r.topic(), r.offset(), r.key(), r.value(), r);

				var val = (Record)r.value();
				/*
				 * Currently there is no simple way to deserialize into a POJO since the classloader
				 * will not see objects from us. Therefore deserialization is done here.
				 */
				person = Person.newBuilder()
						.setId((long)val.get("id"))
						.setFirstname((CharSequence) val.get("firstname"))
						.setLastname((CharSequence) val.get("lastname"))
						.setDob(LocalDate.EPOCH.plusDays((int) val.get("dob")))
						.build();
			}
		}
		catch (Exception e) {
			Ivy.log().error("Could not deserialize.", e);
		}
		return person;
	}
}
