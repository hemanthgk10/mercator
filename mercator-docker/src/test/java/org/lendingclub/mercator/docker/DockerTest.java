package org.lendingclub.mercator.docker;

import org.junit.Test;
import org.lendingclub.mercator.core.BasicProjector;
import org.macgyver.mercator.docker.DockerScanner;
import org.macgyver.mercator.docker.DockerScannerBuilder;

public class DockerTest {

	@Test
	public void testLocalDockerDaemon() {

		try {
			BasicProjector p = new BasicProjector();
			DockerScanner ds = p.createBuilder(DockerScannerBuilder.class).build();
			ds.applyConstraints();
			ds.scan();

		} catch (Exception e) {

			e.printStackTrace();
		}

	}
}
