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
package org.lendingclub.mercator.aws;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.policy.resources.SQSQueueResource;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class SQSScannerTest extends AbstractUnitTest {

	
	@Test
	public void testSerialization() throws JsonProcessingException {
		String account = "123456789012";
		Regions region = Regions.US_WEST_1;
		
		SQSScanner scanner = getProjector().createBuilder(AWSScannerBuilder.class).withAccountId(account).withRegion(region).build(SQSScanner.class);
		
		Map<String,String> queue = Maps.newHashMap();
	
		queue.put("queueArn", "arn:aws:sqs:us-west-1:123456789012:myqueue");
		JsonNode n = scanner.convertSQSQueue(queue, Region.getRegion(region),"https://us-west-1-sqs.amazonaws.com/queue/123456789012/myqueue");
		
		prettyPrint(n);
		Assertions.assertThat(n.path("aws_account").asText()).isEqualTo("123456789012");
		
		Assertions.assertThat(n.path("aws_region").asText()).isEqualTo(region.getName());
		Assertions.assertThat(n.path("aws_account").asText()).isEqualTo(account);
	
		Assertions.assertThat(n.path("aws_arn").asText()).isEqualTo("arn:aws:sqs:us-west-1:123456789012:myqueue");
		Assertions.assertThat(n.path("aws_queueArn").asText()).isEqualTo(n.path("aws_arn").asText());
	
		
	}
}
