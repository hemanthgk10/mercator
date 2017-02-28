package org.lendingclub.mercator.aws;

import org.lendingclub.mercator.core.BasicProjector;
import org.lendingclub.mercator.core.Projector;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AbstractUnitTest {

	static BasicProjector projector = new BasicProjector();
	
	ObjectMapper mapper = new ObjectMapper();
	
	void prettyPrint(JsonNode n) throws JsonProcessingException{
	
		LoggerFactory.getLogger(VPCScanner.class).info("{}",mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n));
	}
	
	Projector getProjector() {
		return projector;
	}
	
	
}
