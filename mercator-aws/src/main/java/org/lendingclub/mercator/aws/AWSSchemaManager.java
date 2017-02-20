package org.lendingclub.mercator.aws;

import org.lendingclub.mercator.core.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.macgyver.neorx.rest.NeoRxClient;

public class AWSSchemaManager extends SchemaManager {

	Logger logger = LoggerFactory.getLogger(AWSSchemaManager.class);




	public AWSSchemaManager(NeoRxClient client) {
		super(client);
		
	}




	public void applyConstraints() {
		applyConstraint("CREATE  CONSTRAINT ON (a:AwsVpc) ASSERT a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsEc2Instance) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsAsg) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsElb) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsRdsInstance) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsDeploymentGroup) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsKinesisStream) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsLaunchConfig) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsSnsTopic) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsSqsQueue) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsSubnet) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsAccount) assert a.aws_account IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsSnsSubscription) assert a.aws_arn IS UNIQUE ");

		applyConstraint("CREATE  CONSTRAINT ON (a:AwsS3Bucket) assert a.aws_arn IS UNIQUE ");

	}

}
