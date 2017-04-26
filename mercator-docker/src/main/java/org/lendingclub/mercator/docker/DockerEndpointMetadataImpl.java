/**
 * Copyright 2017 Lending Club, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.mercator.docker;

import java.util.Optional;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

class DockerEndpointMetadataImpl implements DockerEndpointMetadata {
	static ObjectMapper mapper = new ObjectMapper();
	static Logger logger = LoggerFactory.getLogger(DockerClientSupplier.class);
	DockerClientSupplier supplier;

	public DockerEndpointMetadataImpl(DockerClientSupplier supplier) {
		this.supplier = supplier;
	}

	public Optional<String> getSwarmId() {
		JsonNode n = supplier.getWebTarget().path("/info").request().get(JsonNode.class);

		return Optional.ofNullable(Strings.emptyToNull(n.path("Swarm").path("Cluster").path("ID").asText()));
	}
	@Override
	public EndpointType getEndpointType() {

		JsonNode n = supplier.getWebTarget().path("/info").request().get(JsonNode.class);
	
		
		if (Strings.isNullOrEmpty(n.path("Swarm").path("NodeID").asText(null))) {
			return EndpointType.ENGINE;
		}
	
		if (n.path("ServerVersion").asText().contains("ucp")) {
			
			return EndpointType.UCP;
		}

		return EndpointType.SWARM;

	}
}
