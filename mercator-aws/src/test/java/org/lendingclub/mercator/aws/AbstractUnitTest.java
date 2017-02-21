package org.lendingclub.mercator.aws;

import org.lendingclub.mercator.core.BasicProjector;
import org.lendingclub.mercator.core.Projector;

public class AbstractUnitTest {

	static BasicProjector projector = new BasicProjector();
	
	Projector getProjector() {
		return projector;
	}
	
	
}
