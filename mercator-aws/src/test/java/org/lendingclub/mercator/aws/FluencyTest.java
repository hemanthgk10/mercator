package org.lendingclub.mercator.aws;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.mercator.aws.AWSScannerBuilder;
import org.lendingclub.mercator.aws.AccountScanner;
import org.lendingclub.mercator.core.BasicProjector;

public class FluencyTest {

	@Test
	public void test() {
		BasicProjector p = new BasicProjector();
		
		Assertions.assertThat(p.createBuilder(AWSScannerBuilder.class)).isNotNull();
		Assertions.assertThat(p.createBuilder(AWSScannerBuilder.class).getProjector()).isNotNull();
		AccountScanner scanner = p.createBuilder(AWSScannerBuilder.class).buildAccountScanner();
		
		Assertions.assertThat(scanner.getNeoRxClient()).isSameAs(p.getNeoRxClient());
		Assertions.assertThat(scanner.getProjector()).isSameAs(p);
		
	
	}

}
