package org.lendingclub.mercator.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.macgyver.neorx.rest.NeoRxClient;

public class SchemaManager {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	NeoRxClient client;
	
	public SchemaManager(NeoRxClient client) {
		this.client = client;
	}
	public NeoRxClient getNeoRxClient() {
		return client;
	}
	public void applyConstraints() {
		
	}
	public void applyConstraint(String constraint) {
		applyConstraint(constraint,false);
	}
	public void applyConstraint(String c, boolean failOnError) {
		try {
			logger.info("applying constraint: {}",c);
			client.execCypher(c);
		} catch (RuntimeException e) {
			if (failOnError) {
				throw e;
			} else {
				logger.warn("problem applying constraints: " + c, e);
			}
		}
	}
}
