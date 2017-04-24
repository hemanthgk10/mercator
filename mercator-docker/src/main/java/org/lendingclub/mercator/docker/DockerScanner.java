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

import org.assertj.core.util.Preconditions;
import org.assertj.core.util.Strings;
import org.lendingclub.mercator.core.AbstractScanner;
import org.lendingclub.mercator.core.Scanner;
import org.lendingclub.mercator.core.ScannerBuilder;
import org.lendingclub.mercator.core.ScannerContext;
import org.lendingclub.mercator.core.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;
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

	Supplier<String> dockerIdSupplier = Suppliers.memoize(new DockerManagerIdSupplier());

	public DockerScanner(ScannerBuilder<? extends Scanner> builder) {
		super(builder);

		this.dockerScannerBuilder = (DockerScannerBuilder) builder;

		if (dockerScannerBuilder.managerId != null) {
			dockerIdSupplier = new Supplier<String>() {

				@Override
				public String get() {
					// TODO Auto-generated method stub
					return dockerScannerBuilder.managerId;
				}
			};
		}
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
			getNeoRxClient().execCypher(
					"match (m:DockerHost {mercatorId:{managerId}}), (c:DockerContainer {mercatorId:{containerId}}) MERGE (m)-[r:HOSTS]->(c) set r.updateTs=timestamp()",
					"managerId", hostId, "containerId", c.getId());
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

	private void scanContainersOnEngine() {
		String id = getDockerManagerId();
		new ScannerContext().exec(x -> {
			getDockerClient().listContainersCmd().withShowAll(true).exec().forEach(it -> {
				try {

					projectContainer(it, id);
				} catch (Exception e) {
					ScannerContext.getScannerContext().ifPresent(ctx -> {
						ctx.markException(e);

					});
					logger.warn("", e);
				}

			});
		});
	}

	public DockerEndpointType getEndpointType(DockerClient client) {
		Info info = client.infoCmd().exec();
		if (info == null) {
			logger.info("info was null");
			return DockerEndpointType.ENGINE;
		} else {
			List<Object> systemStatus = info.getSystemStatus();
			if (systemStatus != null) {
				for (Object it : systemStatus) {
					try {
						if (it instanceof List) {
							List tuple = (List) it;

							if (tuple.size() > 0 && tuple.get(0).toString().contains("Cluster Managers")) {
								return DockerEndpointType.UCP;
							}

						}
					} catch (RuntimeException e) {
						logger.warn("problem", e);
					}
				}
			} else {
				logger.info("system status is null...so we are talking to an engine");
				return DockerEndpointType.ENGINE;
			}
		}
		return DockerEndpointType.ENGINE;
	}

	public void scanInfo() {
		Info info = getDockerClient().infoCmd().exec();

		if (info != null) {
			String id = info.getId();
			if (id != null) {
				if (getEndpointType(getDockerClient()) == DockerEndpointType.UCP) {
					getNeoRxClient().execCypher("merge (x:DockerManager {mercatorId:{id}}) set x.updateTs=timestamp()",
							"id", id);
				}
				else {
					getNeoRxClient().execCypher("merge (x:DockerHost {mercatorId:{id}}) set x.updateTs=timestamp()",
							"id", id);
				}
			}

		}

	}

	public void scanImages() {
		getDockerClient().listImagesCmd().exec().forEach(img -> {

			// Note that images don't really belong to a cluster. They are
			// unique across time and space by SHA256 and may be present
			// anywhere.
			getProjector().getNeoRxClient().execCypher("merge (x:DockerImage {mercatorId:{id}}) set x+={props}", "id",
					img.getId(), "props", mapper.valueToTree(img));
		});
	}

	public void scan() {

		scanInfo();
		
		DockerEndpointType type = getEndpointType(getDockerClient());
		logger.info("endpoint type: {}",type);
		scanImages();
		
		if (type == DockerEndpointType.UCP) {
			scanUCPManager();
		}
		else if (type==DockerEndpointType.ENGINE) {
			scanContainersOnEngine();
		}
		else {
			logger.warn("unknown endpoint type: {}",type);
		}
		
		

	}

	JsonNode getClusterSummary() {
		Info info = getDockerClient().infoCmd().exec();
		if (info == null) {
			return NullNode.instance;
		}
		boolean inNodesSection = false;
		List<Object> statusList = info.getSystemStatus();
		if (statusList == null) {
			return NullNode.instance;
		}
		ObjectNode summary = mapper.createObjectNode();
		ArrayNode nodes = mapper.createArrayNode();
		summary.set("nodes", nodes);
		ObjectNode currentNode = null;
		String id = info.getId();
		summary.put("id", id);
		for (Object it : statusList) {
			List tuple = (List) it;
			if (tuple != null && tuple.size() > 0) {
				String a = tuple.get(0).toString();

				if (a.equals("Nodes")) {
					inNodesSection = true;
				} else if (a.length() > 0 && a.charAt(0) != ' ') {
					inNodesSection = false;
				}
				if (!a.equals("Nodes")) {
					if (inNodesSection) {
						if (a.charAt(1) != ' ') {
							currentNode = mapper.createObjectNode();
							currentNode.put("Name", tuple.get(0).toString().trim());
							String address = tuple.get(1).toString().trim();
							if (!address.startsWith("tcp://")) {
								address = "tcp://" + address;
							}
							currentNode.put("Address", address);
							nodes.add(currentNode);
						} else if (a.startsWith("  └ ")) {
							if (currentNode != null) {
								a = a.replace("  └ ", "").trim();
								String val = tuple.get(1).toString().trim();
								currentNode.put(a, val);
							}
						}

					}
				}
			}
		}

		return summary;
	}

	protected void scanUCPManager() {
		JsonNode summary = getClusterSummary();

		String clusterId = summary.path("id").asText(null);
		summary.path("nodes").forEach(it -> {
			try {

				ObjectNode neo4jNode = mapper.createObjectNode();
				neo4jNode.put("name", it.path("Name").asText());
				neo4jNode.put("address", it.path("Address").asText());
				neo4jNode.put("serverVersion", it.path("ServerVersion").asText());
				neo4jNode.put("id", it.path("ID").asText());

				String nodeId = neo4jNode.path("id").asText();
				String cypher = "merge (a:DockerHost {mercatorId:{mercatorId}}) set a+={props}, a.updateTs=timestamp()";
				getNeoRxClient().execCypher(cypher, "mercatorId", neo4jNode.path("id").asText(), "props", neo4jNode);
				scanUCPManagedDockerHost(neo4jNode.get("address").asText(),nodeId);
				if (clusterId != null) {
					cypher = "match (m:DockerManager {mercatorId:{managerId}}), (h:DockerHost {mercatorId:{hostId}}) merge (m)-[:MANAGES]->(h)";
					getNeoRxClient().execCypher(cypher, "managerId", clusterId, "hostId",
							neo4jNode.path("id").asText());
				}
			} catch (Exception e) {
				logger.warn("unexpected exception", e);
			}
		});

	}

	private void scanUCPManagedDockerHost(String address, String nodeId) {
		DockerClientSupplier cs = getDockerClientSupplier().newBuilder().withDockerHost(address).build();

		for (Container container : cs.get().listContainersCmd().exec()) {
			try {
				projectContainer(container, nodeId);
			} catch (Exception e) {
				logger.warn("", e);
			}
		}

	}

	@Override
	public SchemaManager getSchemaManager() {
		return new DockerSchemaManager(getProjector().getNeoRxClient());
	}

	/**
	 * This should be an identifier that is unique over time and space. If we
	 * are talking to a local docker daemon, it should be the unique id of that
	 * daemon. If we are talking to a swarm cluster, it should be unique
	 * identifier for the cluster. THis might need some adjustment. We will see.
	 * 
	 * @return
	 */
	public String getDockerManagerId() {
		return dockerIdSupplier.get();
	}

	class DockerManagerIdSupplier implements Supplier<String> {

		@Override
		public String get() {
			return getDockerClient().infoCmd().exec().getId();
		}

	}

}
