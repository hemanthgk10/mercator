package org.lendingclub.mercator.newrelic;

import java.util.Map;

import org.lendingclub.mercator.core.ScannerBuilder;

import com.google.common.collect.Maps;

public class NewRelicScannerBuilder extends ScannerBuilder<NewRelicScanner>{
	
	private Map<String, String> props = Maps.newConcurrentMap();
	
	public NewRelicScannerBuilder withToken(String token) {
		props.put("newrelic.token", token);
		return this;
	}
	
	public NewRelicScannerBuilder withAccountId(String accountId) {
		props.put("newrelic.accountId", accountId);
		return this;
	}

	@Override
	public NewRelicScanner build() {
		return new NewRelicScanner(this, props);
	}

}
