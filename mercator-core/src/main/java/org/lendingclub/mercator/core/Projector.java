package org.lendingclub.mercator.core;

import java.util.Map;

import io.macgyver.neorx.rest.NeoRxClient;

public interface Projector {
	
	public Map<String,String> getProperties();
	public NeoRxClient getNeoRxClient();
	public <T extends ScannerBuilder> T createBuilder(Class<T> clazz);
	
}
