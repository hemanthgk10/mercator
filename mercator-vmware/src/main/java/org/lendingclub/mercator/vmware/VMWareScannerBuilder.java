package org.lendingclub.mercator.vmware;

import java.util.Map;

import org.lendingclub.mercator.core.ScannerBuilder;

import com.google.common.collect.Maps;

public class  VMWareScannerBuilder extends ScannerBuilder<VMWareScanner> {

	Map<String,String> props = Maps.newConcurrentMap();
	
	
	
	
	public VMWareScannerBuilder withUrl(String url) {
		props.put("vmware.url", url);
		return this;
	}
	
	public VMWareScannerBuilder withUsername(String username) {
		props.put("vmware.username", username);
		return this;
	}
	
	public VMWareScannerBuilder withPassword(String password) {
		props.put("vmware.password", password);
		return this;
	}
	
	@Override
	public VMWareScanner build() {

		
		return new VMWareScanner(this, props);
		
	}


}
