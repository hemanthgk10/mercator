package org.lendingclub.mercator.core;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import io.macgyver.neorx.rest.NeoRxClient;

public abstract class AbstractScanner implements Scanner {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	ScannerBuilder<? extends Scanner> builder;
	Map<String, String> config;

	public AbstractScanner(ScannerBuilder<? extends Scanner> builder, Map<String, String> props) {
		this.builder = builder;
		config = Maps.newHashMap();
		config.putAll(builder.getProjector().getProperties());
		if (props != null) {
			config.putAll(props);
		}
	}

	public Map<String, String> getConfig() {
		return config;
	}

	@Override
	public Projector getProjector() {
		return builder.getProjector();
	}

	public boolean isFailOnError() {
		return builder.isFailOnError();
	}
	
	
	public void maybeThrow(Exception e, String message) {
		ScannerContext.getScannerContext().ifPresent(sc -> {
			sc.markException(e);
		});
		if (isFailOnError()) {
			if (e instanceof RuntimeException) {
				throw ((RuntimeException) e);
			}
			else {
				throw new MercatorException(e);
			}
		}
		else {
			logger.warn(message,e);
		}
	}
	
	public void maybeThrow(Exception e) {
		maybeThrow(e,"scanning problem");
		
	}
	
	public SchemaManager getSchemaManager() {
		return new SchemaManager(getProjector().getNeoRxClient());
	}
	
}
