package org.lendingclub.mercator.docker;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.mercator.docker.DockerClientSupplier.Builder;

public class DockerClientSupplierTest {

	
	@Test
	public void testSupplier() {
		
		Builder b = 
		new DockerClientSupplier.Builder();
		
		b.assertNotImmutable();
		
		b.withName("foo");
		
		DockerClientSupplier cs = b.build();
		
		try {
			b.build();
			Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
		}
		catch (Exception e) {
			Assertions.assertThat(e).isInstanceOf(IllegalStateException.class);
		}
		
		b = cs.newBuilder();
		b.assertNotImmutable();
	}
}
