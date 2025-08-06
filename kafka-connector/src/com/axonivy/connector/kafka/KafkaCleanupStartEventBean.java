/**
 * 
 */
package com.axonivy.connector.kafka;

import org.eclipse.core.runtime.IProgressMonitor;

import ch.ivyteam.ivy.process.eventstart.AbstractProcessStartEventBean;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventBean;
import ch.ivyteam.ivy.process.eventstart.IProcessStartEventBeanRuntime;
import ch.ivyteam.ivy.process.extension.ProgramConfig;
import ch.ivyteam.ivy.service.ServiceException;
import ch.ivyteam.log.Logger;

/**
 * {@link IProcessStartEventBean} to listen to Apache Kafka topics.
 */
public class KafkaCleanupStartEventBean extends AbstractProcessStartEventBean {
	public KafkaCleanupStartEventBean() {
		super("KafkaCleanupStartEventBean", "Kafka Cleanup Bean");
	}

	@Override
	public void initialize(IProcessStartEventBeanRuntime eventRuntime, ProgramConfig configuration) {
		super.initialize(eventRuntime, configuration);
		eventRuntime.poll().disable();
		log().debug("Initializing KafkaCleanupStartEventBean.");
	}

	@Override
	public synchronized void start(IProgressMonitor monitor) throws ServiceException {
		super.start(monitor);
		log().info("Started KafkaCleanupStartEventBean.");
	}

	@Override
	public synchronized void stop(IProgressMonitor monitor) throws ServiceException {
		KafkaService.get().closeAllProducers();
		super.stop(monitor);
		log().info("Stopped KafkaCleanupStartEventBean.");
	}

	@Override
	public void poll() {
		log().warn("Did not expect call to poll (polling was disabled in KafkaCleanupStartEventBean).");
	}

	protected Logger log() {
		return getEventBeanRuntime().getRuntimeLogLogger();
	}
}
