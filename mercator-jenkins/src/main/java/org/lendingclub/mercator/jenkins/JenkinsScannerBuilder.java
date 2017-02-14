package org.lendingclub.mercator.jenkins;

import java.util.Map;

import org.lendingclub.mercator.core.ScannerBuilder;

import com.google.common.collect.Maps;

public class JenkinsScannerBuilder extends ScannerBuilder<JenkinsScanner>{

	Map<String,String> props = Maps.newHashMap();
	
	public JenkinsScannerBuilder() {
		
	}

	public JenkinsScannerBuilder withUrl(String url) {
		props.put("jenkins.url", url);
		return this;
	}
	
	public JenkinsScannerBuilder withUsername(String username) {
		props.put("jenkins.username", username);
		return this;
	}
	
	public JenkinsScannerBuilder withPassword(String password) {
		props.put("jenkins.password", password);
		return this;
	}
	
	
	@Override
	public JenkinsScanner build() {
	
		return new JenkinsScanner(this, props);
	}

}
