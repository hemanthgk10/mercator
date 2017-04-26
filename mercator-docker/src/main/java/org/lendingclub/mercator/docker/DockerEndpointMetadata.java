package org.lendingclub.mercator.docker;

import java.util.Optional;

public interface DockerEndpointMetadata {
	
	
	
	public static DockerEndpointMetadata discover(DockerClientSupplier supplier) {
		DockerEndpointMetadata md = new DockerEndpointMetadataImpl(supplier);
		return md;
	}
	public static enum EndpointType {

		ENGINE,
		SWARM,
		UCP
	}
	public Optional<String> getSwarmId();
	public EndpointType getEndpointType();
}
