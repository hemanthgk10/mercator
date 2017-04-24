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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.assertj.core.util.Strings;
import org.lendingclub.mercator.core.MercatorException;
import org.lendingclub.mercator.core.ScannerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class DockerScannerBuilder extends ScannerBuilder<DockerScanner> {

	static Logger logger = LoggerFactory.getLogger(DockerScannerBuilder.class);
	
	String LOCAL_DOCKER_DAEMON = "unix:///var/run/docker.sock";
	@Override
	public DockerScanner build() {		
		return new DockerScanner(this);
	}
	String managerId;

	List<Consumer<Builder>> configList = Lists.newArrayList();
	Supplier<DockerClient> dockerClientSupplier = null;
	
	public DockerScannerBuilder() {
		
	}
	
	/**
	 * Sets a synthetic id for the swarm.  There may be cases where the id of the cluster can't be obtained and we want to set it.
	 * @param id
	 * @return
	 */
	public DockerScannerBuilder withSwarmId(String id) {
		this.managerId = id;
		return this;
	}
	
	public DockerScannerBuilder withLocalDockerDaemon() {
		return withDockerHost(LOCAL_DOCKER_DAEMON);
	}
	public DockerScannerBuilder withDockerHost(String host) {
		return withConfig(cfg->{
			logger.info("using docker host: {}",host);
			cfg.withDockerHost(host);
		});
	}
	public DockerScannerBuilder withClientConfig(File f) {
		logger.info("loading config from: {}",f);
		JsonNode n = loadDockerConfig(f);
		
		if (!n.isObject()) {
			throw new MercatorException("could not load config from: "+f);
		}
		
		String dockerHost = n.path("DOCKER_HOST").asText();
		String certPath = n.path("DOCKER_CERT_PATH").asText(null);
		String tlsVerify = n.path("DOCKER_TLS_VERIFY").asText("1").trim();
	
		boolean isVerifyEnabled = tlsVerify.equals("1") || tlsVerify.toLowerCase().equals("true");
		if (Strings.isNullOrEmpty(dockerHost)) {
			throw new MercatorException("DOCKER_HOST not found in "+f);
		}
		if (Strings.isNullOrEmpty(certPath)) {
			throw new MercatorException("DOCKER_CERT_PATH not found in "+f);
		}
		return withDockerHost(dockerHost).withConfig(cfg->{
			
			if (!Strings.isNullOrEmpty(certPath)) {
				
				cfg.withDockerCertPath(certPath);
				cfg.withDockerTlsVerify(isVerifyEnabled);
			}
		});

	}
	public DockerScannerBuilder withConfig(Consumer<Builder> cfg) {
		this.configList.add(cfg);
		return this;
	}
	
	public DockerScannerBuilder withDockerClientSupplier(Supplier<DockerClient> supplier) {
		this.dockerClientSupplier = supplier;
		return this;
	}
	public DockerScannerBuilder withDockerClient(DockerClient client) {
		this.dockerClientSupplier = new Supplier<DockerClient>() {

			@Override
			public DockerClient get() {
				return client;
			}
			
		};
		return this;
	}
	
	public static JsonNode loadDockerConfig(File dir) {
	
		Map<String, String> map = Maps.newHashMap();
		Pattern p = Pattern.compile("^\\s*export\\s*(.*)=(.*)\\s*");
		if (dir != null && dir.isDirectory()) {
			try {
				File envFile = new File(dir, "env.sh");
				if (envFile.isFile()) {
					Files.readAllLines(new File(dir, "env.sh").toPath()).forEach(line -> {
						Matcher m = p.matcher(line);
						if (m.matches()) {
							String key = m.group(1);
							String val = m.group(2);
							if (val.contains("$(pwd)")) {
								val = dir.getAbsolutePath();
							}
							map.put(key, val);
						}
					});
					JsonNode n = new ObjectMapper().convertValue(map, JsonNode.class);
					
					
					return n;
				}
			} catch (IOException e) {
				logger.warn("", e);
			}
		}
		return NullNode.getInstance();
	}
}
