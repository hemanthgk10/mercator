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
import java.util.concurrent.atomic.AtomicBoolean;

import org.lendingclub.mercator.core.AbstractScanner;
import org.lendingclub.mercator.core.Scanner;
import org.lendingclub.mercator.core.ScannerBuilder;
import org.lendingclub.mercator.core.ScannerContext;
import org.lendingclub.mercator.core.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class DockerScanner extends AbstractScanner {

	Logger logger = LoggerFactory.getLogger(DockerScanner.class);

	DockerScannerBuilder dockerScannerBuilder;

	static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.registerModule(new DockerSerializerModule());
	}
	Supplier<DockerClient> supplier;

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
		if (dockerScannerBuilder.dockerClientSupplier != null) {
			this.supplier = adapt(dockerScannerBuilder.dockerClientSupplier);
		} else {
			supplier = new DockerClientSupplier();
		}

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

	class DockerClientSupplier implements Supplier<DockerClient> {
		public DockerClient get() {

			Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();

			dockerScannerBuilder.configList.forEach(c -> {
				c.accept(builder);
			});

			DefaultDockerClientConfig cc = builder.build();

			DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory().withReadTimeout(5000)
					.withConnectTimeout(5000).withMaxTotalConnections(100).withMaxPerRouteConnections(10);

			DockerClient dockerClient = DockerClientBuilder.getInstance(cc)
					.withDockerCmdExecFactory(dockerCmdExecFactory).build();

			return dockerClient;
		}

	}

	public DockerClient getDockerClient() {
		return supplier.get();
	}

	protected void projectContainer(Container c) {
		JsonNode n = mapper.valueToTree(c);

		String cypher = "merge (c:DockerContainer {mercatorId:{id}}) set c+={props},c.updateTs=timestamp() return c";

		getProjector().getNeoRxClient().execCypher(cypher, "id", c.getId(), "props", n);

		getNeoRxClient().execCypher(
				"match (m:DockerManager {mercatorId:{managerId}}), (c:DockerContainer {mercatorId:{containerId}}) MERGE (m)-[r:MANAGES]->(c) set r.updateTs=timestamp()",
				"managerId", getDockerManagerId(), "containerId", c.getId());

		getNeoRxClient().execCypher(
				"match (m:DockerHost {mercatorId:{managerId}}), (c:DockerContainer {mercatorId:{containerId}}) MERGE (m)-[r:HOSTS]->(c) set r.updateTs=timestamp()",
				"managerId", getDockerManagerId(), "containerId", c.getId());

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

	public void scanContainers() {
		new ScannerContext().exec(x -> {
			getDockerClient().listContainersCmd().withShowAll(true).exec().forEach(it -> {
				try {

					projectContainer(it);
				} catch (Exception e) {
					ScannerContext.getScannerContext().ifPresent(ctx -> {
						ctx.markException(e);

					});
					logger.warn("", e);
				}

			});
		});
	}


	private boolean isUCP(Info info) {
		AtomicBoolean b = new AtomicBoolean(false);
		if (info == null) {
			return false;
		}
		List<Object> systemStatus = info.getSystemStatus();
		if (systemStatus != null) {
			info.getSystemStatus().forEach(it -> {
				if (it.toString().contains("Orca Controller")) {
					b.set(true);
				}
			});
		}
		return true;
	}

	public void scanInfo() {
		Info info = getDockerClient().infoCmd().exec();
	
		
	
		if (info != null) {
			String id = info.getId();
			if (id != null) {
				if (isUCP(info)) {
					getNeoRxClient().execCypher("merge (x:DockerManager {mercatorId:{id}}) set x.updateTs=timestamp()",
							"id", id);
				} else {
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
		scanImages();
		scanContainers();

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
