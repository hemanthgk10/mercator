package org.lendingclub.mercator.aws;

import org.junit.Test;
import org.lendingclub.mercator.aws.AMIScanner;
import org.lendingclub.mercator.aws.AWSScannerBuilder;

import com.amazonaws.regions.Regions;

public class AMIScannerIntegrationTest extends AWSIntegrationTest {

	@Test
	public void testIt() {


		
		AWSScannerBuilder b = new AWSScannerBuilder()
				.withProjector(getProjector()).withRegion(Regions.US_WEST_2);

		b.build(AMIScanner.class);

	}
}
