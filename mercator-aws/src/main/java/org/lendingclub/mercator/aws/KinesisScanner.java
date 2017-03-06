package org.lendingclub.mercator.aws;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.DescribeStreamResult;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.ListStreamsResult;
import com.amazonaws.services.kinesis.model.StreamDescription;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class KinesisScanner extends AWSScanner<AmazonKinesisClient> {

	public KinesisScanner(AWSScannerBuilder builder) {
		super(builder,AmazonKinesisClient.class,"AwsKinesisStream");
	}

	@Override
	protected AmazonKinesisClient createClient() {
		return (AmazonKinesisClient) builder.configure(AmazonKinesisClientBuilder
				.standard()).build();
	}

	@Override
	protected void doScan() {

		ListStreamsResult result = getClient().listStreams();
		
		for(String name: result.getStreamNames()) {
			try {
				scanStream(name);
			}
			catch (RuntimeException e) {
				maybeThrow(e,"problem scanning kinesis");
			}
		}

		
	}

	private void scanStream(String name) {
		com.amazonaws.services.kinesis.model.DescribeStreamResult result = getClient().describeStream(name);
		StreamDescription description = result.getStreamDescription();
		
		project(description);
		
	}
	private void project(StreamDescription description) {

		
		ObjectNode n = mapper.createObjectNode();
		n.put("aws_account", getAccountId());
		n.put("aws_region", getRegion().getName());
		n.put("aws_arn",description.getStreamARN());
		n.put("name",description.getStreamName());
		n.put("aws_name", description.getStreamName());
		n.put("aws_status", description.getStreamStatus());
		n.put("aws_streamCreationTimestamp",description.getStreamCreationTimestamp().getTime());
		n.put("aws_retentionPeriodHours",description.getRetentionPeriodHours());
		n.put("aws_shardCount", description.getShards().size());
		
		incrementEntityCount();
		String cypher = "merge (k:AwsKinesisStream {aws_arn:{aws_arn}}) set k+={props}, k.updateTs=timestamp() return k";

		getNeoRxClient().execCypher(cypher, "aws_arn", n.path("aws_arn").asText(), "props", n);

		cypher = "match (a:AwsAccount {aws_account:{account}}), (k:AwsKinesisStream {aws_account:{account}}) MERGE (a)-[r:OWNS]->(k) set r.updateTs=timestamp()";

		getNeoRxClient().execCypher(cypher, "account", getAccountId());

	}


}
