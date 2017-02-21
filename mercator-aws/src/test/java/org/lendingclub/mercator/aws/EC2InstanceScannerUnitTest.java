package org.lendingclub.mercator.aws;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.lendingclub.mercator.core.MercatorException;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EC2InstanceScannerUnitTest extends AbstractUnitTest {

	ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testIt() throws JsonProcessingException {
		EC2InstanceScanner scanner = getProjector().createBuilder(AWSScannerBuilder.class).withAccountId("111222333444")
				.build(EC2InstanceScanner.class);

		Instance test = new Instance();
		test.setInstanceId("i-123456789012");
		test.setImageId("ami-01010101");

		Tag tag = new Tag("foo", "bar");
		Tag tag2 = new Tag("fizz", "buzz");
		test.setTags(Lists.newArrayList(tag, tag2));

		ObjectNode n = scanner.convertAwsObject(test, Region.getRegion(Regions.US_WEST_2));

		System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(n));

		Assertions.assertThat(n.path("aws_region").asText()).isEqualTo("us-west-2");
		Assertions.assertThat(n.path("aws_account").asText()).isEqualTo("111222333444");
		Assertions.assertThat(n.path("aws_arn").asText())
				.isEqualTo("arn:aws:ec2:us-west-2:111222333444:instance/i-123456789012");
		Assertions.assertThat(n.path("aws_imageId").asText()).isEqualTo("ami-01010101");
		Assertions.assertThat(n.path("aws_tag_foo").asText()).isEqualTo("bar");
		Assertions.assertThat(n.path("aws_tag_fizz").asText()).isEqualTo("buzz");

	}

	@Test
	public void testFailOnErrorDisabled() {
		EC2InstanceScanner scanner = getProjector().createBuilder(AWSScannerBuilder.class).withAccountId("111222333444")
				.build(EC2InstanceScanner.class);
		Assertions.assertThat(scanner.isFailOnError()).isFalse();
		scanner.maybeThrow(new RuntimeException("foo"));
	}

	@Test
	public void testFailOnErrorEnable() {

		EC2InstanceScanner scanner = getProjector().createBuilder(AWSScannerBuilder.class).withAccountId("111222333444")
				.withFailOnError(true).build(EC2InstanceScanner.class);
		try {
			Assertions.assertThat(scanner.isFailOnError()).isTrue();
			scanner.maybeThrow(new RuntimeException("foo"));
		} catch (RuntimeException e) {
			Assertions.assertThat(e).hasMessageContaining("foo");
		}
		
		try {
			Assertions.assertThat(scanner.isFailOnError()).isTrue();
			scanner.maybeThrow(new IOException("foo"));
		} catch (RuntimeException e) {
			Assertions.assertThat(e).hasMessageContaining("foo");
			Assertions.assertThat(e).isInstanceOf(MercatorException.class).hasCauseExactlyInstanceOf(IOException.class);
		}
	}

}
