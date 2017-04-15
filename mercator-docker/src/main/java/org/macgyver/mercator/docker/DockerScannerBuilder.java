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
package org.macgyver.mercator.docker;

import java.util.List;
import java.util.function.Consumer;

import org.lendingclub.mercator.core.ScannerBuilder;

import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.google.common.collect.Lists;


public class DockerScannerBuilder extends ScannerBuilder<DockerScanner> {

	String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";
	@Override
	public DockerScanner build() {		
		return new DockerScanner(this);
	}


	List<Consumer<Builder>> configList = Lists.newArrayList();

	public DockerScannerBuilder() {
		withDockerHost(DEFAULT_DOCKER_HOST);
	}
	public DockerScannerBuilder withDockerHost(String host) {
		return withConfig(cfg->{
			cfg.withDockerHost(host);
		});
	}
	public DockerScannerBuilder withConfig(Consumer<Builder> cfg) {
		this.configList.add(cfg);
		return this;
	}
	

}
