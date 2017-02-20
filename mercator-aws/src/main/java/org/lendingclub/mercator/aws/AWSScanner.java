package org.lendingclub.mercator.aws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.lendingclub.mercator.core.AbstractScanner;
import org.lendingclub.mercator.core.Projector;
import org.lendingclub.mercator.core.ProjectorException;
import org.lendingclub.mercator.core.Scanner;
import org.lendingclub.mercator.core.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.client.builder.AwsSyncClientBuilder;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import io.macgyver.neorx.rest.NeoRxClient;

public abstract class AWSScanner<T extends AmazonWebServiceClient> extends AbstractScanner {

	static ObjectMapper mapper = new ObjectMapper();

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private Projector projector;
	private AmazonWebServiceClient client;
	private Region region;

	private ScannerMetricCollector metricCollector = new ScannerMetricCollector();
	protected AWSScannerBuilder builder;
	JsonConverter converter;
	
	Class<? extends AmazonWebServiceClient> clientType;
	
	public static final String AWS_REGION_ATTRIBUTE="aws_region";

	public AWSScanner(AWSScannerBuilder builder, Class<? extends AmazonWebServiceClient> clientType) {
		super(builder,Maps.newHashMap());
		if (builder.getRegion()==null) {
			builder.withRegion(Region.getRegion(Regions.US_EAST_1));
		}
		this.clientType = clientType;

		Preconditions.checkNotNull(builder);
		Preconditions.checkNotNull(builder.getRegion());
		Preconditions.checkNotNull(builder.getProjector());
		
		this.builder = builder;
		this.region = builder.getRegion();
		this.projector = builder.getProjector();
		
		

	}

	protected <T extends AmazonWebServiceClient> T createClient(Class<T> clazz) {
		try {
			String builderClass = clazz.getName() + "Builder";
			Class<? extends AwsSyncClientBuilder> builderClazz = (Class<? extends AwsSyncClientBuilder>) Class
					.forName(builderClass);
			Method m = builderClazz.getMethod("standard");
			return (T) builder.configure((AwsSyncClientBuilder) m.invoke(null)).build();
		} catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new ProjectorException(e);
		}

	}
	protected  T createClient() {
		return (T) createClient(clientType);
	}
	
	public Projector getProjector() {
		return projector;
	}
	@SuppressWarnings("unchecked")
	public T getClient() {
		if (this.client==null) {
			this.client = createClient();
		}
		return (T) this.client;
	}

	public Region getRegion() {
		
		return region;
	}

	public String getAccountId() {
		return (String) builder.getAccountIdSupplier().get();
	}


	public NeoRxClient getNeoRxClient() {
		return projector.getNeoRxClient();
	}
	public final void scan() {
	
		long t0 = System.currentTimeMillis();
		logger.info("start scan - account={} region={}",getAccountId(),getRegion());
		doScan();
		long t1 = System.currentTimeMillis();
		logger.info("end scan - account={} region={} duration={} ms",getAccountId(),getRegion(),t1-t0);
	}
	protected abstract void doScan();

	public ObjectNode convertAwsObject(Object x, Region region) {
		ObjectNode n = JsonConverter.newInstance(getAccountId(), region).toJson(x,null);
		Optional<String> arn = computeArn(n);
		if (arn.isPresent()) {
			n.put("aws_arn", arn.get());
		}
		if (region!=null) {
			n.put("aws_region", region.getName());
		}
		n.put("aws_account", getAccountId());
		return n;
		
	}

	public GraphNodeGarbageCollector newGarbageCollector() {
		return new GraphNodeGarbageCollector().neo4j(getNeoRxClient()).account(getAccountId());
	}

	public Optional<String> computeArn(JsonNode n) {

		return Optional.empty();

	}



	@Override
	public SchemaManager getSchemaManager() {
		return new AWSSchemaManager(getNeoRxClient());
	}

	public RequestMetricCollector getMetricCollector() {
		return metricCollector;
	}
}
