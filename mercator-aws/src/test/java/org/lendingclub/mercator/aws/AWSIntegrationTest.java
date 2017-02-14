package org.lendingclub.mercator.aws;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.lendingclub.mercator.core.BasicProjector;
import org.lendingclub.mercator.core.Projector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.common.collect.Maps;

public abstract class AWSIntegrationTest {

	static Logger logger = LoggerFactory.getLogger(AWSIntegrationTest.class);
	static Projector projector;

	synchronized Projector getProjector() {
		if (projector == null) {
		
			Projector p = new BasicProjector(Maps.newHashMap());
			this.projector = p;
		}
		return projector;
	}



	@BeforeClass
	public static void setup() {

		try {
			GetCallerIdentityResult r = AWSSecurityTokenServiceClientBuilder
					.standard().build().getCallerIdentity(new GetCallerIdentityRequest());

			logger.info("account: {}",r.getAccount());
			logger.info("userId:  {}",r.getUserId());
			logger.info("arn:     {}",r.getArn());
			
			Assume.assumeTrue(true);
		} catch (Exception e) {
			logger.warn("AWS Integration tests will be skipped",e);
			Assume.assumeTrue(false);
		}

	}

}
