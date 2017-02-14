package org.lendingclub.mercator.github;

import java.util.Map;

import org.lendingclub.mercator.core.ScannerBuilder;

import com.google.common.collect.Maps;

public class  GitHubScannerBuilder extends ScannerBuilder<GitHubScanner> {

	Map<String,String> props = Maps.newConcurrentMap();
	
	
	
	public GitHubScannerBuilder withToken(String token) {
		props.put("github.token", token);
		return this;
	}

	public GitHubScannerBuilder withUrl(String url) {
		props.put("github.url", url);
		return this;
	}
	
	public GitHubScannerBuilder withUsername(String username) {
		props.put("github.username", username);
		return this;
	}
	
	public GitHubScannerBuilder withPassword(String password) {
		props.put("github.password", password);
		return this;
	}
	
	@Override
	public GitHubScanner build() {

		
		return new GitHubScanner(this, props);
		
	}


}
