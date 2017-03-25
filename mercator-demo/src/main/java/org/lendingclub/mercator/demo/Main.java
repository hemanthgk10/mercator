/**
 * Copyright 2017 Lending Club, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
