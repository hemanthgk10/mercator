package org.lendingclub.mercator.docker;

import org.junit.Assume;

public class DockerIntegrationTest {

	DockerClientSupplier getLocalDaemonClientSupplier() {
		try {
			DockerClientSupplier supplier = new DockerClientSupplier.Builder().withLocalEngine().build();

			supplier.get().infoCmd().exec();

			return supplier;
		} catch (Exception e) {
			Assume.assumeTrue(false);
		}
		
		throw new IllegalStateException();
	}
}
