package org.lendingclub.mercator.aws;

public class AllEntityScanner extends AWSScannerGroup {

	public AllEntityScanner(AWSScannerBuilder builder) {
		super(builder);
		
		addScannerType(AccountScanner.class);
		addScannerType(RegionScanner.class);
		addScannerType(AvailabilityZoneScanner.class);
		
		addScannerType(SubnetScanner.class);
		addScannerType(VPCScanner.class);
		addScannerType(SecurityGroupScanner.class);
		addScannerType(AMIScanner.class);
		addScannerType(EC2InstanceScanner.class);
		addScannerType(ELBScanner.class);
		addScannerType(LaunchConfigScanner.class);
		addScannerType(ASGScanner.class);
		addScannerType(RDSInstanceScanner.class);
		addScannerType(S3BucketScanner.class);
		addScannerType(SQSScanner.class);
		addScannerType(SNSScanner.class);
		
		// addScannerType(KinesisScanner.class);  // Kinesis seems to be heavily rate-limited...leave it off for now
		
	}


}
