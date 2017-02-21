package org.lendingclub.mercator.aws;

import java.util.List;
import java.util.Optional;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

public class S3BucketScanner extends AWSScanner<AmazonS3Client> {

	public S3BucketScanner(AWSScannerBuilder builder) {
		super(builder,AmazonS3Client.class);
	}

	@Override
	protected AmazonS3Client createClient() {
		return (AmazonS3Client) builder.configure(AmazonS3ClientBuilder
				.standard()).build();
	}

	@Override
	protected void doScan() {
		AmazonS3Client client = getClient();
		
		
		List<Bucket> bucketList = client.listBuckets();
		
		for (Bucket bucket: bucketList) {
			scanBucket(bucket);
		}
		
	}

	
	@Override
	public Optional<String> computeArn(JsonNode n) {
		String name = n.path("name").asText(null);
		
		if (Strings.isNullOrEmpty(name)) {
			return Optional.empty();
		}
		return Optional.of("arn:aws:s3:::"+name);
	}

	private void scanBucket(Bucket b) {
	
	
		ObjectNode props = mapper.createObjectNode();
		props.put("name", b.getName());
		
		props.put("aws_arn", computeArn(props).orElse(null));
		props.put("aws_account", getAccountId());
		

		String cypher = "merge (b:AwsS3Bucket { name:{name} }) set b+={props}, b.updateTs=timestamp()";
		
		getNeoRxClient().execCypher(cypher, "name",b.getName(), "props",props).forEach(r->{
			getShadowAttributeRemover().removeTagAttributes("AwsS3Bucket", props, r);
		});
		
		cypher = "match (a:AwsAccount {aws_account:{account}}), (b:AwsS3Bucket {aws_account:{account}}) MERGE (a)-[r:OWNS]->(b) set r.updateTs=timestamp()";
		
		getNeoRxClient().execCypher(cypher, "account",getAccountId());
		
	
	}
}
