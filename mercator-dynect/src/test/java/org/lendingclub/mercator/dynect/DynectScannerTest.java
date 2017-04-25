package org.lendingclub.mercator.dynect;

import org.junit.Ignore;
import org.junit.Test;
import org.lendingclub.mercator.core.Projector;

public class DynectScannerTest {
	
	@Test
	@Ignore
	public void test() {
		
		String companyName = "LendingClub";
		String userName = "mercator";
		String password = "test";
		
		DynectScanner scanner = new Projector.Builder().build().createBuilder(DynectScannerBuilder.class).withProperties(companyName, userName, password).build();
		scanner.scan();	
	}
}
