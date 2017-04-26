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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.Response;

import org.lendingclub.mercator.core.AbstractScanner;
import org.lendingclub.mercator.core.Scanner;
import org.lendingclub.mercator.core.ScannerBuilder;
import org.lendingclub.mercator.core.ScannerContext;
import org.lendingclub.mercator.core.SchemaManager;
import org.lendingclub.mercator.docker.DockerEndpointMetadata.EndpointType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class DockerScanner extends AbstractScanner {

	Logger logger = LoggerFactory.getLogger(DockerScanner.class);

	DockerScannerBuilder dockerScannerBuilder;

	static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.registerModule(new DockerSerializerModule());
	}
	DockerClientSupplier supplier;

	public DockerScanner(ScannerBuilder<? extends Scanner> builder) {
		super(builder);

		this.dockerScannerBuilder = (DockerScannerBuilder) builder;

		this.supplier = dockerScannerBuilder.dockerClientSupplierBuilder.build();

	}

	private Supplier<DockerClient> adapt(java.util.function.Supplier<DockerClient> s) {
		Supplier adapter = new Supplier<DockerClient>() {

			@Override
			public DockerClient get() {
				return s.get();
			}

		};
		return adapter;
	}

	public DockerClientSupplier getDockerClientSupplier() {
		return supplier;
	}

	public DockerClient getDockerClient() {
		Preconditions.checkNotNull(supplier);
		return supplier.get();
	}

	protected void projectContainer(Container c, String hostId) {
		JsonNode n = mapper.valueToTree(c);

		String cypher = "merge (c:DockerContainer {mercatorId:{id}}) set c+={props},c.updateTs=timestamp() return c";

		getProjector().getNeoRxClient().execCypher(cypher, "id", c.getId(), "props", n);

		if (!Strings.isNullOrEmpty(hostId)) {

		}

		cypher = "merge (x:DockerImage {mercatorId:{imageId}}) set x.updateTs=timestamp() return x";

		getProjector().getNeoRxClient().execCypher(cypher, "imageId", c.getImageId(), "name", c.getImage());

		cypher = "match (di:DockerImage {mercatorId:{imageId}}), (dc:DockerContainer {mercatorId:{id}}) merge (dc)-[r:USES]->(di) set r.updateTs=timestamp()";

		getProjector().getNeoRxClient().execCypher(cypher, "imageId", c.getImageId(), "id", c.getId());

		ScannerContext.getScannerContext().ifPresent(it -> {
			it.incrementEntityCount();
			it.incrementEntityCount();
		});

		projectContainerDetail(c);
	}

	private void projectContainerDetail(Container c) {
		InspectContainerResponse icr = getDockerClient().inspectContainerCmd(c.getId()).exec();

		String cypher = "merge (c:DockerContainer {mercatorId:{id}}) set c+={props}";
		getProjector().getNeoRxClient().execCypher(cypher, "id", c.getId(), "props", mapper.valueToTree(icr));

	}

	private JsonNode flattenContainer(JsonNode n) {
		ObjectNode fn = mapper.createObjectNode();
		n.fields().forEachRemaining(it -> {
			if (it.getValue().isValueNode()) {
				fn.set(it.getKey(), it.getValue());
			}
		});
		n.path("Labels").fields().forEachRemaining(it -> {
			String labelKey = "label_" + it.getKey();
			fn.set(labelKey, it.getValue());
		});
		fn.set("mercatorId", n.path("Id"));
		return fn;
	}

	protected void scanContainersOnEngine(DockerClientSupplier engineSupplier, String nodeId) {

		scanInfo(engineSupplier, nodeId);
		JsonNode n = engineSupplier.getWebTarget().path("/containers/json").queryParam("all", "1").request()
				.get(JsonNode.class);

		n.forEach(it -> {
			JsonNode fn = flattenContainer(it);

			String cypher = "merge (c:DockerContainer {mercatorId:{id}}) set c+={props},c.updateTs=timestamp() return c";

			String containerId = fn.path("Id").asText();
			getProjector().getNeoRxClient().execCypher(cypher, "id", containerId, "props", fn);

			String imageId = fn.get("Image").asText();
			cypher = "merge (x:DockerImage {mercatorId:{imageId}}) set x.updateTs=timestamp() return x";

			getProjector().getNeoRxClient().execCypher(cypher, "imageId", imageId);

			cypher = "match (di:DockerImage {mercatorId:{imageId}}), (dc:DockerContainer {mercatorId:{id}}) merge (dc)-[r:USES]->(di) set r.updateTs=timestamp()";

			getProjector().getNeoRxClient().execCypher(cypher, "imageId", imageId, "id", containerId);

			String hostId = engineSupplier.getWebTarget().path("/info").request().get(JsonNode.class).path("ID")
					.asText();

			getNeoRxClient().execCypher(
					"match (m:DockerHost {mercatorId:{managerId}}), (c:DockerContainer {mercatorId:{containerId}}) MERGE (m)-[r:HOSTS]->(c) set r.updateTs=timestamp()",
					"managerId", hostId, "containerId", containerId);
		});

	}

	private JsonNode flattenInfo(JsonNode n) {
		ObjectNode fn = mapper.createObjectNode();

		n.fields().forEachRemaining(it -> {
			if (it.getValue().isValueNode()) {
				fn.set(it.getKey(), it.getValue());
			}
		});
		return fn;
	}

	public void scanInfo(DockerClientSupplier supplier, String nodeId) {

		JsonNode n = supplier.getWebTarget().path("/info").request().get(JsonNode.class);

		EndpointType type = supplier.getEndpointMetadata().getEndpointType();

		if (type == EndpointType.UCP) {
			String swarmId = n.path("Swarm").path("Cluster").path("ID").asText();
			if (!Strings.isNullOrEmpty(swarmId)) {
				getNeoRxClient().execCypher(
						"merge (x:DockerSwarm {mercatorId:{id}}) set x+={props}, x.updateTs=timestamp()", "id", swarmId,
						"props", flattenInfo(n));
			}
		} else if (type == EndpointType.ENGINE || type == EndpointType.SWARM) {

			if (type == EndpointType.SWARM) {
				Optional<String> swarmId = supplier.getEndpointMetadata().getSwarmId();
				if (swarmId.isPresent()) {
					// we NO NOT put the endpoint's properties here because we are talking to a swarm manager.  We need to find a way to make this as consistent
					// as possible with UCP
					getNeoRxClient().execCypher(
							"merge (x:DockerSwarm {mercatorId:{id}}) set x.updateTs=timestamp()", "id",
							swarmId.get());
				}
			}
			String id = n.path("ID").asText();

			getNeoRxClient().execCypher("merge (x:DockerHost {mercatorId:{id}}) set x+={props},x.updateTs=timestamp()",
					"id", id, "props", flattenInfo(n));

			if (!Strings.isNullOrEmpty(nodeId)) {
				getNeoRxClient().execCypher("match (x:DockerHost {mercatorId:{id}}) set x.nodeId={nodeId}", "id", id,
						"nodeId", nodeId);
			}
		} else {
			throw new IllegalStateException();
		}

	}

	void prettyInfo(String msg, JsonNode n) {
		try {
			logger.info("{} \n {}", msg, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n));
		} catch (JsonProcessingException e) {
			logger.warn("", e);
		}
	}

	private JsonNode flattenImage(JsonNode n) {
		ObjectNode fn = mapper.createObjectNode();
		n.fields().forEachRemaining(it -> {
			if (it.getValue().isValueNode()) {
				fn.set(it.getKey(), it.getValue());
			}
		});
		n.path("Labels").fields().forEachRemaining(it -> {
			String labelKey = "label_" + it.getKey();
			fn.set(labelKey, it.getValue());
		});
		fn.set("mercatorId", n.path("Id"));
		return fn;
	}

	public void scanImages() {
		JsonNode n = getDockerClientSupplier().getWebTarget().path("/images/json").request().get(JsonNode.class);

		if (n.isArray()) {
			n.forEach(it -> {
				// Note that images don't really belong to a cluster. They are
				// unique across time and space by SHA256 and may be present
				// anywhere.

				JsonNode image = flattenImage(it);

				getProjector().getNeoRxClient().execCypher(
						"merge (x:DockerImage {mercatorId:{id}}) set x+={props},x.updateTs=timestamp()", "id",
						image.path("mercatorId").asText(), "props", image);
			});
		}

	}

	public void scan() {

		scanInfo(getDockerClientSupplier(), null);

		EndpointType type = getDockerClientSupplier().getEndpointMetadata().getEndpointType();
		logger.info("scanning endpoint type: {}", type);
		scanImages();

		if (type == EndpointType.UCP || type == EndpointType.SWARM) {
			scanContainersOnUCPManager();
		} else if (type == EndpointType.ENGINE) {
			scanContainersOnEngine(getDockerClientSupplier(), null);
		} else {
			logger.warn("unknown endpoint type: {}", type);
		}

	}

	protected void scanContainersOnUCPManager() {

		Optional<String> swarmID = getDockerClientSupplier().getEndpointMetadata().getSwarmId();

		// Ideally we could get all this information from UCP...but due to some
		// feature, it seem like it is difficult/impossible to get
		// it to tell you which node is hosting the container.
		getDockerClientSupplier().getWebTarget().path("/nodes").request().get(JsonNode.class).forEach(it -> {
			try {
				String id = it.path("ID").asText();
				String ip = it.path("Status").path("Addr").asText();
				logger.info("scanning node: {}", id);

				String engineAddress = "tcp://" + ip + ":12376";
				logger.info("connecting to node={} via {}", id, engineAddress);
				DockerClientSupplier engine = getDockerClientSupplier().newBuilder().withDockerHost(engineAddress)
						.build();

				scanContainersOnEngine(engine, id);

				if (swarmID.isPresent()) {
					logger.info("connecting swarm={} to node={}", swarmID.get(), id);
					getProjector().getNeoRxClient().execCypher(
							"match (s:DockerSwarm {mercatorId:{swarmId}}),(h:DockerHost {nodeId:{nodeId}}) merge (s)-[r:CONTAINS]->(h)",
							"swarmId", swarmID.get(), "nodeId", id);
				}

			} catch (RuntimeException e) {
				logger.warn("problem scanning engine", e);
			}
		});

	}

	@Override
	public SchemaManager getSchemaManager() {
		return new DockerSchemaManager(getProjector().getNeoRxClient());
	}

}
