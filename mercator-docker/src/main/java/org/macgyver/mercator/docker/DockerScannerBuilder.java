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
