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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.ws.rs.client.WebTarget;

import org.lendingclub.mercator.core.MercatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

public class DockerClientSupplier implements Supplier<DockerClient> {

	private static final String CACHE_KEY="singleton";
	static Logger logger = LoggerFactory.getLogger(DockerClientSupplier.class);
	//static AtomicInteger counter = new AtomicInteger(0);
	Builder builder;
	
	Cache<String,DockerClient> cache = CacheBuilder.newBuilder().expireAfterWrite(10,TimeUnit.MINUTES).build();
	protected DockerClientSupplier() {
		
	}
	public String getName() {
		return builder.name;
	}

	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", getName()).toString();
	}

	public Builder newBuilder() {
		Builder nb = new Builder();
		nb.configList.addAll(builder.configList);
		nb.cmdExecFactoryConfigList.addAll(builder.cmdExecFactoryConfigList);
		nb.name = builder.name;
		return nb;
	}
	
	public static class Builder {
		List<Consumer<com.github.dockerjava.core.DefaultDockerClientConfig.Builder>> configList = Lists.newArrayList();
		List<Consumer<DockerCmdExecFactory>> cmdExecFactoryConfigList = Lists.newArrayList();
		AtomicReference<DockerClientSupplier> clientRef = new AtomicReference<DockerClientSupplier>(null);
	
		String name;

		public void assertNotImmutable() {
			if (clientRef.get()!=null) {
				throw new IllegalStateException("build() has already been called");
			}
		}
		public <T extends Builder> T withDockerConfigDir(File path) {
			assertNotImmutable();
			Optional<JsonNode> x =  DockerScannerBuilder.loadDockerConfig(path);
			if (!x.isPresent()) {
				throw new MercatorException("could not load config from: "+path);
			}

			return (T) withClientConfig(x.get());
		}

		public <T extends Builder> T withClientConfig(JsonNode n) {
			assertNotImmutable();
		
			String host = n.path("DOCKER_HOST").asText();
			if (!Strings.isNullOrEmpty(host)) {
		
				withDockerHost(host);
			}
			
			String certPath = n.path("DOCKER_CERT_PATH").asText();
			if (!Strings.isNullOrEmpty(certPath)) {
				withBuilderConfig(cfg ->{
					cfg.withDockerCertPath(certPath);
				});
			}
			String verify = n.path("DOCKER_TLS_VERIFY").asText();
			if (!Strings.isNullOrEmpty(verify)) {
				if (verify.trim().toLowerCase().equals("1")) {
					withBuilderConfig(cfg ->{
					
						cfg.withDockerTlsVerify(true);
					});
					
				}
				else {
					withBuilderConfig(cfg ->{
						cfg.withDockerTlsVerify(false);
					});
				}
			}
			return (T) this;
		}
		public <T extends Builder> T withCertPath(String path) {
			assertNotImmutable();
			return withCertPath(new File(path));
		}

		public <T extends Builder> T withLocalEngine() {
			assertNotImmutable();
			return withDockerHost("unix:///var/run/docker.sock");


		}

		public <T extends Builder> T withCertPath(File path) {
			assertNotImmutable();
			return (T) withBuilderConfig(b -> {
				b.withDockerCertPath(path.getAbsolutePath());
			});
		}



		public <T extends Builder> T withDockerHost(String host) {
			assertNotImmutable();
			return withBuilderConfig(cfg->{
				cfg.withDockerHost(host);
			});
		
		}
		public <T extends Builder> T withName(String name) {
			assertNotImmutable();
			this.name = name;
			return (T) this;
		}

		public <T extends Builder> T withJerseyConfig(Consumer<JerseyDockerCmdExecFactory> c ) {
			Object object = c;
			Consumer<DockerCmdExecFactory> cc = (Consumer<DockerCmdExecFactory>)object;
			
			cmdExecFactoryConfigList.add(cc);
			return (T) this;
		}
		public <T extends Builder> T withBuilderConfig(
				Consumer<com.github.dockerjava.core.DefaultDockerClientConfig.Builder> consumer) {
			assertNotImmutable();
			configList.add(consumer);
			return (T) this;
		}

		public synchronized <T extends DockerClientSupplier> T build() {
			assertNotImmutable();
			if (Strings.isNullOrEmpty(name)) {
				throw new IllegalStateException("withName() must be set");
			}
			DockerClientSupplier cs = new DockerClientSupplier();
			cs.builder = this;
			clientRef.set(cs);
			return (T) cs;
		}
	}

	protected DockerClient create() {
		com.github.dockerjava.core.DefaultDockerClientConfig.Builder b = DefaultDockerClientConfig
				.createDefaultConfigBuilder();

		for (Consumer<com.github.dockerjava.core.DefaultDockerClientConfig.Builder> consumer : this.builder.configList) {
			consumer.accept(b);
		}
		
		
		DefaultDockerClientConfig cfg = b.build();

		DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
				  .withReadTimeout(3000)
				  .withConnectTimeout(3000)
				  .withMaxTotalConnections(100)
				  .withMaxPerRouteConnections(10);
		
		this.builder.cmdExecFactoryConfigList.forEach(it->{
			it.accept(dockerCmdExecFactory);
		});
		
		return DockerClientBuilder.getInstance(cfg).withDockerCmdExecFactory(dockerCmdExecFactory).build();
	}

	public void reset() {
		cache.invalidateAll();
	}
	@Override
	public DockerClient get() {
		DockerClient client = cache.getIfPresent(CACHE_KEY);
		if (client!=null) {
			return client;
		}
		client = create();
		cache.put(CACHE_KEY, client);
		return client;
	}

	/**
	 * The Docker java client is significantly behind the server API.  Rather than try to fork/patch our way to success, 
	 * we just implement a bit of magic to get access to the underlying jax-rs WebTarget.
	 * 
	 * Docker should just expose this as a public method.
	 * 
	 * @return
	 */
	public WebTarget getWebTarget() {
		return extractWebTarget(get());
	}
	
	/**
	 * The Docker java client is significantly behind the server API.  Rather than try to fork/patch our way to success, 
	 * we just implement a bit of magic to get access to the underlying jax-rs WebTarget.
	 * 
	 * Docker should just expose this as a public method.
	 * 
	 * @param c
	 * @return
	 */
	public static WebTarget extractWebTarget(DockerClient c)  {

		try {
			for (Field m : DockerClientImpl.class.getDeclaredFields()) {
				
				if (DockerCmdExecFactory.class.isAssignableFrom(m.getType())) {
					m.setAccessible(true);
					JerseyDockerCmdExecFactory f = (JerseyDockerCmdExecFactory) m.get(c);
					Method method = f.getClass().getDeclaredMethod("getBaseResource");
					method.setAccessible(true);
					return (WebTarget) method.invoke(f);
				}
			}
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new IllegalStateException("could not obtain WebTarget",e);
		}
		throw new IllegalStateException("could not obtain WebTarget");
	}
}
