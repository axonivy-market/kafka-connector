package com.axonivy.connector.kafka.demo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.avro.data.TimeConversions;
import org.apache.avro.data.TimeConversions.DateConversion;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.axonivy.connector.kafka.KafkaService;

import ch.ivyteam.ivy.environment.Ivy;

public class DemoService {
	private static final DemoService INSTANCE = new DemoService();
	private static final DateConversion DATE_CONVERSION = new TimeConversions.DateConversion();
	private static final Random RND = new Random();
	private static final List<String> countries = List.of("AT", "DE", "CH");
	private static long personId;

	public static DemoService get() {
		return INSTANCE;
	}

	public Person createRandomPerson() {
		var person = Person.newBuilder()
				.setId(++personId)
				.setFirstname(randomCapitalized(5, 10))
				.setLastname(randomCapitalized(5, 15))
				.setDob(LocalDate.now().minusDays(365 * (10 + RND.nextInt(90))))
				.setAddresses(new ArrayList<>())
				.build();

		var count = RND.nextInt(1, 4);

		while(count-- > 0) {
			person.getAddresses().add(createRandomAddress());
		}
		return person;
	}

	public Address createRandomAddress() {
		return Address.newBuilder()
				.setCountry(countries.get(RND.nextInt(countries.size())))
				.setZipCode("%d".formatted(1000 + RND.nextInt(9000)))
				.setStreet("%s %d".formatted(randomCapitalized(5, 10), RND.nextInt(1, 200)))
				.setCity(randomCapitalized(5, 10))
				.build();
	}

	public String randomCapitalized(int min, int max) {
		return StringUtils.capitalize(RandomStringUtils.randomAlphabetic(min, max+1).toLowerCase());
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

				/*
				 * Read the object as a GenericData.Record instead of automatic deserialisiation
				 * into a Person object. Reason is, that deserialisation into a Person object would
				 * require most of the time some classloader hacking.
				 * 
				 * To use automatic de-serialisation, set specific.avro.reader: true and make sure,
				 * that the needed objects can be loaded by producer.send() and consumer.poll().
				 * 
				 * This will likely require to set a thread context classloader before send() and poll(). 
				 * Note, that classloader results are cached in a static map and will be used for
				 * sending and receiving. Make sure, that the thread context classloader is set
				 * before the first send() or poll().
				 * 
				 * Thread.currentThread().setContextClassLoader(MyObject.class.getClassLoader());
				 * 
				 * Note: Consuming objects with a StartEventBean currently requires to inherit and
				 * implement your own version to set the thread context classloader before the first
				 * poll!
				var persRec = (Record)r.value();

				person = Person.newBuilder()
						.setId((long)persRec.get("id"))
						.setFirstname((CharSequence) persRec.get("firstname"))
						.setLastname((CharSequence) persRec.get("lastname"))
						.setDob(DATE_CONVERSION.fromInt((int) persRec.get("dob"), Person.SCHEMA$, LogicalTypes.date()))
						.setAddresses(new ArrayList<>())
						.build();

				@SuppressWarnings("unchecked")
				var adrsRec = (Array<Record>)persRec.get("addresses");

				for (var adrRec : adrsRec) {
					person.getAddresses().add(
							Address.newBuilder()
							.setStreet((CharSequence) adrRec.get("street"))
							.setZipCode((CharSequence) adrRec.get("zipCode"))
							.setCity((CharSequence) adrRec.get("city"))
							.setCountry((CharSequence) adrRec.get("country"))
							.build()
							);
				}
				 */

				person = (Person)r.value();
			}
		}
		catch (Exception e) {
			Ivy.log().error("Could not deserialize.", e);
		}
		return person;
	}
}
