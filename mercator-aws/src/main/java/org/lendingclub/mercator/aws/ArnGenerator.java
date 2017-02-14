package org.lendingclub.mercator.aws;

import java.util.Optional;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.google.common.base.Preconditions;

public class ArnGenerator {

	Region region;
	String account;
	
	public static ArnGenerator newInstance(String account, Region r) {
		ArnGenerator g = new ArnGenerator();
		g.account = account;
		g.region = r;
		return g;
	}
	public static ArnGenerator newInstance(String account, String region) {
		region=region.toUpperCase().replace("-", "_");
		return newInstance(account, Region.getRegion(Regions.valueOf(region)));
	}
	public static ArnGenerator newInstance(String account, Regions region) {
		return newInstance(account, Region.getRegion(region));
	}

	public String createEc2InstanceArn(String instanceId) {
		return String.format("arn:aws:ec2:%s:%s:instance/%s",region.toString(),account,instanceId);
	}
	
	public String createElbArn(String name) {
		return String.format("arn:aws:elasticloadbalancing:%s:%s:loadbalancer/%s",region.toString(),account,
			name);
	}
	
	public String createAmiArn(String imageId) {
		return String.format("arn:aws:ec2:%s::image/%s", region.toString(),imageId);
	}
}
