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

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.mercator.aws.ArnGenerator;
import org.lendingclub.mercator.aws.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

public class Neo4jJsonConverterTest  {

	Logger logger = LoggerFactory.getLogger(JsonConverter.class);
	ObjectMapper mapper = new ObjectMapper();
	
	JsonConverter converter;
	ArnGenerator arnGenerator;
	
	protected void setup(String account, Regions region) {
		converter = JsonConverter.newInstance(account, region);
		arnGenerator = ArnGenerator.newInstance(account, region);
	}
	@Test
	public void testEc2Instance() throws IOException {
		setup("12345",Regions.US_WEST_2);
		Instance instance = new Instance();
		instance.setInstanceId("i-123456");
		
		
		instance.setTags(Lists.newArrayList(new Tag("foo", "bar"),new Tag("fizz","buzz")));
		JsonNode n = converter.toJson(instance,arnGenerator.createEc2InstanceArn(instance.getInstanceId()));
		
		Assertions.assertThat(n.path("aws_instanceId").asText()).isEqualTo("i-123456");
		Assertions.assertThat(n.path("aws_arn").asText()).isEqualTo("arn:aws:ec2:us-west-2:12345:instance/i-123456");
		Assertions.assertThat(n.path("aws_region").asText()).isEqualTo("us-west-2");
		Assertions.assertThat(n.path("aws_account").asText()).isEqualTo("12345");
		Assertions.assertThat(n.path("aws_tag_foo").asText()).isEqualTo("bar");
		Assertions.assertThat(n.path("aws_tag_fizz").asText()).isEqualTo("buzz");
		logger.info("{}",mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n));
	}

}
