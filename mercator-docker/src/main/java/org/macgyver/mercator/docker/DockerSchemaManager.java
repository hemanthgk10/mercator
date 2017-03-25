package org.macgyver.mercator.docker;

import org.lendingclub.mercator.core.SchemaManager;

import io.macgyver.neorx.rest.NeoRxClient;

public class DockerSchemaManager extends SchemaManager {

	public DockerSchemaManager(NeoRxClient client) {
		super(client);
		
	}

	@Override
	public void applyConstraints() {
		applyConstraint("CREATE CONSTRAINT ON (a:DockerImage) assert a.id IS UNIQUE ");
		
		applyConstraint("CREATE CONSTRAINT ON (a:DockerContainer) assert a.id IS UNIQUE ");
	}

}
