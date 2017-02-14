package org.lendingclub.mercator.aws;


import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.google.common.base.Preconditions;

public abstract class AbstractEC2Scanner extends AWSScanner<AmazonEC2Client> {

	

	public AbstractEC2Scanner(AWSScannerBuilder builder) {
		super(builder, AmazonEC2Client.class);
		Preconditions.checkNotNull(builder.getProjector());
	}
		

}
