package org.lendingclub.mercator.aws;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.mercator.aws.ArnGenerator;

import com.amazonaws.regions.Regions;

public class ArnGeneratorTest {

	@Test
	public void testIt() {

		ArnGenerator g = ArnGenerator.newInstance("123456789", Regions.US_WEST_2);
		
		Assertions.assertThat(g.createElbArn("foo")).isEqualTo("arn:aws:elasticloadbalancing:us-west-2:123456789:loadbalancer/foo");

		Assertions.assertThat(g.createEc2InstanceArn("i-123456")).isEqualTo("arn:aws:ec2:us-west-2:123456789:instance/i-123456");

	}

}
