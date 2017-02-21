package org.lendingclub.mercator.aws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.lendingclub.mercator.core.AbstractScanner;
import org.lendingclub.mercator.core.Projector;
import org.lendingclub.mercator.core.MercatorException;
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
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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

	ShadowAttributeRemover shadowRemover;
	public static final String AWS_REGION_ATTRIBUTE = "aws_region";
	public static final String AWS_ACCOUNT_ATTRIBUTE="aws_account";
	public static final String AWS_ARN_ATTRIBUTE="aws_arn";

	public AWSScanner(AWSScannerBuilder builder, Class<? extends AmazonWebServiceClient> clientType) {
		super(builder, Maps.newHashMap());
		if (builder.getRegion() == null) {
			builder.withRegion(Region.getRegion(Regions.US_EAST_1));
		}
		this.clientType = clientType;

		Preconditions.checkNotNull(builder);
		Preconditions.checkNotNull(builder.getRegion());
		Preconditions.checkNotNull(builder.getProjector());

		this.builder = builder;
		this.region = builder.getRegion();
		this.projector = builder.getProjector();

		this.shadowRemover = new ShadowAttributeRemover(getNeoRxClient());

	}

	protected ShadowAttributeRemover getShadowAttributeRemover() {
		return shadowRemover;
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
			throw new MercatorException(e);
		}

	}

	protected T createClient() {
		return (T) createClient(clientType);
	}

	public Projector getProjector() {
		return projector;
	}

	@SuppressWarnings("unchecked")
	public T getClient() {
		if (this.client == null) {
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
		try {
			
			logger.info("{} started scan", toString());

			doScan();
			long t1 = System.currentTimeMillis();
			logger.info("{} ended scan duration={} ms", this, t1 - t0);
		} catch (RuntimeException e) {
			logger.warn("{} aborted scan duration={} ms", this, System.currentTimeMillis() - t0);
			maybeThrow(e);
		}
	}

	protected abstract void doScan();

	public ObjectNode convertAwsObject(Object x, Region region) {
		ObjectNode n = JsonConverter.newInstance(getAccountId(), region).toJson(x, null);
		Optional<String> arn = computeArn(n);
		if (arn.isPresent()) {
			n.put(AWS_ARN_ATTRIBUTE, arn.get());
		}
		if (region != null) {
			n.put(AWS_REGION_ATTRIBUTE, region.getName());
		}
		n.put(AWS_ACCOUNT_ATTRIBUTE, getAccountId());
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

	public String toString() {
		String safeAccount = "unknown";
		try {
			safeAccount = getAccountId();
		} catch (Exception e) {
			// getAccountId() could trigger a callout...bad thing to happen from
			// toString()
		}
		return MoreObjects.toStringHelper(this).add(AWS_REGION_ATTRIBUTE, getRegion().getName()).add(AWS_ACCOUNT_ATTRIBUTE, safeAccount)
				.toString();
	}

	protected boolean tokenHasNext(String token) {
		return (!Strings.isNullOrEmpty(token)) && (!token.equals("null"));

	}
	
	
	
}
