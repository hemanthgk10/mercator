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

import javax.ws.rs.client.WebTarget;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.mercator.docker.DockerClientSupplier.Builder;
import org.lendingclub.mercator.docker.DockerEndpointMetadata.EndpointType;

public class DockerClientSupplierTest extends DockerIntegrationTest {

	@Test
	public void testSupplier() {

		Builder b = new DockerClientSupplier.Builder();

		b.assertNotImmutable();

		b.withName("foo");

		DockerClientSupplier cs = b.build();

		try {
			b.build();
			Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
		} catch (Exception e) {
			Assertions.assertThat(e).isInstanceOf(IllegalStateException.class);
		}

		b = cs.newBuilder();
		b.assertNotImmutable();
	}

	@Test
	public void testJerseyWebTarget() {

		// This will work even if docker isn't available. It tests that the
		// reflection tricks work.
		Assertions
				.assertThat(
						new DockerClientSupplier.Builder().withName("local").withLocalEngine().build().getWebTarget())
				.isNotNull();

	}
	

}
