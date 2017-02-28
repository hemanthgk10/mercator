package org.lendingclub.mercator.aws;

import java.util.Map;
import java.util.Optional;

import com.amazonaws.regions.Region;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SQSScanner extends AWSScanner<AmazonSQSClient> {

	public SQSScanner(AWSScannerBuilder builder) {
		super(builder, AmazonSQSClient.class);

	}

	@Override
	protected AmazonSQSClient createClient() {
		return createClient(AmazonSQSClient.class);
	}

	@Override
	protected void doScan() {

		ListQueuesResult result = getClient().listQueues();

		for (String url : result.getQueueUrls()) {
			try {
				scanQueue(url);
			} catch (RuntimeException e) {
				maybeThrow(e);
			}
		}
	}

	private void scanQueue(String url) {
		GetQueueAttributesResult result = getClient().getQueueAttributes(url, Lists.newArrayList("All"));
		projectQueue(url, result.getAttributes());
	}

	@Override
	public Optional<String> computeArn(JsonNode n) {
		return Optional.ofNullable(n.path("aws_queueArn").asText(null));
	}

	public ObjectNode convertSQSQueue(Map<String, String> map, Region region, String url) {
		Map<String, String> target = Maps.newHashMap();

	
		map.forEach((k, v) -> {
			if (k.length() > 1) {
				String modifiedKey = "aws_" + Character.toLowerCase(k.charAt(0)) + k.substring(1, k.length());
				target.put(modifiedKey, v);
			}
		});

		ObjectNode n = mapper.createObjectNode();
		n.put("aws_account", getAccountId());
		n.put("aws_region", getRegion().getName());
		n.put("url", url);
		n.put("aws_arn", map.get("queueArn"));
		target.forEach((k, v) -> {

			n.put(k, v);
		});
		return n;
	}

	@Override
	public ObjectNode convertAwsObject(Object x, Region region) {
		Map<String, String> map = (Map<String, String>) x;
		return convertSQSQueue(map,region,null);
	}

	private void projectQueue(String url, Map<String, String> attrsOrig) {

		ObjectNode n = convertSQSQueue(attrsOrig, getRegion(), url);

		String cypher = "merge (t:AwsSqsQueue {aws_arn:{aws_arn}}) set t+={props}, t.updateTs=timestamp() return t";

		getNeoRxClient().execCypher(cypher, "aws_arn", n.path("aws_arn").asText(), "props", n).forEach(r -> {
			getShadowAttributeRemover().removeTagAttributes("AwsSqsQueue", n, r);
		});

		cypher = "match (a:AwsAccount {aws_account:{account}}), (q:AwsSqsQueue {aws_account:{account}}) MERGE (a)-[r:OWNS]->(q) set r.updateTs=timestamp()";

		getNeoRxClient().execCypher(cypher, "account", getAccountId());

	}

}
