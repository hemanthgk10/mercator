package org.macgyver.mercator.docker;

import java.util.Map;

import org.lendingclub.mercator.core.AbstractScanner;
import org.lendingclub.mercator.core.Scanner;
import org.lendingclub.mercator.core.ScannerBuilder;
import org.lendingclub.mercator.core.ScannerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

	public DockerScanner(ScannerBuilder<? extends Scanner> builder, Map<String, String> props) {
		super(builder, props);

		supplier = Suppliers.memoize(new DockerClientSupplier());
		this.dockerScannerBuilder = (DockerScannerBuilder) builder;
		
	}

	class DockerClientSupplier implements Supplier<DockerClient> {
		public DockerClient get() {
			String host = getConfig().getOrDefault("DOCKER_HOST", "unix:///var/run/docker.sock");
			Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(host);
			
			
			DefaultDockerClientConfig cc  =		builder.build();

			if (dockerScannerBuilder.configurator!=null) {
				dockerScannerBuilder.configurator.accept(builder);
			}
			
			DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory().withReadTimeout(1000)
					.withConnectTimeout(1000).withMaxTotalConnections(100).withMaxPerRouteConnections(10);

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

		String cypher = "merge (c:DockerContainer {id:{id}}) set c+={props},c.updateTs=timestamp() return c";

		getProjector().getNeoRxClient().execCypher(cypher, "id", c.getId(), "props", n);

		cypher = "merge (x:DockerImage {id:{imageId}}) set x.updateTs=timestamp() return x";

		getProjector().getNeoRxClient().execCypher(cypher, "imageId", c.getImageId(), "name", c.getImage());

		cypher = "match (di:DockerImage {id:{imageId}}), (dc:DockerContainer {id:{id}}) merge (dc)-[r:USES]->(di) set r.updateTs=timestamp()";

		getProjector().getNeoRxClient().execCypher(cypher, "imageId", c.getImageId(), "id", c.getId());

		ScannerContext.getScannerContext().ifPresent(it -> {
			it.incrementEntityCount();
			it.incrementEntityCount();
		});
		
		projectContainerDetail(c);
	}
	
	private void projectContainerDetail(Container c) {
		InspectContainerResponse icr = getDockerClient().inspectContainerCmd(c.getId()).exec();
		
		String cypher = "merge (c:DockerContainer {id:{id}}) set c+={props}";
		getProjector().getNeoRxClient().execCypher(cypher, "id",c.getId(),"props",mapper.valueToTree(icr));
		
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

	

	
	public void scanInfo() {
		Info info = getDockerClient().infoCmd().exec();

		ObjectNode n = (ObjectNode) mapper.valueToTree(info);

		
	}

	public void scanImages() {
		getDockerClient().listImagesCmd().exec().forEach(img -> {
			
		
			getProjector().getNeoRxClient().execCypher("merge (x:DockerImage {id:{id}}) set x+={props}", "id",
					img.getId(), "props", mapper.valueToTree(img));
		});
	}


	public void scan() {
		scanInfo();
		scanImages();
		scanContainers();

	}

	public void applyConstraints() {
		DockerSchemaManager m = new DockerSchemaManager(getProjector().getNeoRxClient());
		m.applyConstraints();
	}
}
