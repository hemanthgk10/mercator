package org.lendingclub.mercator.aws;

import java.util.List;

import com.amazonaws.AmazonWebServiceClient;
import com.google.common.collect.Lists;

//@SuppressWarnings("rawtypes")
public class AWSScannerGroup extends AWSScanner<AmazonWebServiceClient> {


	@SuppressWarnings("rawtypes")
	List<Class<? extends AWSScanner>> scannerList = Lists.newCopyOnWriteArrayList();
	
	
	public AWSScannerGroup(AWSScannerBuilder builder) {
		super(builder,null);
	}
	@SuppressWarnings("rawtypes")
	public List<Class<? extends AWSScanner>> getScannerTypes() {
		return scannerList;
	}
	@SuppressWarnings("rawtypes")
	public AWSScannerGroup addScannerType(Class<? extends AWSScanner> type) {
		scannerList.add(type);
		return this;
	}
	@SuppressWarnings("rawtypes")
	public AWSScannerGroup removeScannerType(Class<? extends AWSScanner> type) {
		scannerList.remove(type);
		return this;
	}
	
	
	@Override
	@SuppressWarnings("rawtypes")
	protected void doScan() {

		for (Class<? extends AWSScanner> scanner: scannerList) {
			scan(scanner);
		}
	}

	@SuppressWarnings("rawtypes")
	protected void scan(Class<? extends AWSScanner> clazz) {
		try {
			AWSScannerBuilder.class.cast(builder.withFailOnError(isFailOnError())).build(clazz).scan();					
		} catch (RuntimeException e) {
			maybeThrow(e);
		}
	}
}
