package org.lendingclub.mercator.demo;

import org.lendingclub.mercator.aws.AWSScannerBuilder;
import org.lendingclub.mercator.aws.AllEntityScanner;
import org.lendingclub.mercator.core.BasicProjector;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.amazonaws.regions.Regions;

public class Main {

	
	
	public static void main(String [] args) {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		BasicProjector projector = new BasicProjector();
		projector.createBuilder(AWSScannerBuilder.class).withRegion(Regions.US_EAST_1).build(AllEntityScanner.class).scan();
		projector.createBuilder(AWSScannerBuilder.class).withRegion(Regions.US_WEST_2).build(AllEntityScanner.class).scan();
		
		System.out.println("");
		System.out.println("* * * * * * *");
		System.out.println("Point your browser to: http://localhost:7474");
		
	}

}
