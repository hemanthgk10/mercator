package org.macgyver.mercator.docker;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import org.lendingclub.mercator.core.ScannerBuilder;

import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;


public class DockerScannerBuilder extends ScannerBuilder<DockerScanner> {

	@Override
	public DockerScanner build() {		
		return new DockerScanner(this,getProjector().getProperties());
	}

	Consumer<Builder> configurator;
	
	public DockerScannerBuilder withConfig(Consumer<Builder> cfg) {
		this.configurator = cfg;
		return this;
	}
	

	
	
}
