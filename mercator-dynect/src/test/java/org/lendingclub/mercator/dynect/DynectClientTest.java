package org.lendingclub.mercator.dynect;

import org.junit.Ignore;
import org.junit.Test;

public class DynectClientTest {
	
	DynectClient client;
	
	@Test
	@Ignore
	public void test() {
		
		client = new DynectClient.DynBuilder().withProperties("LendingClub", "mercator", "test").build();
		client.get("Zone");

	}

}
