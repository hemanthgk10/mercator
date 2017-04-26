package org.lendingclub.mercator.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonLogger {

	static Logger logger = LoggerFactory.getLogger(JsonLogger.class);
	static ObjectMapper mapper = new ObjectMapper();

	public static void logInfo(String msg, JsonNode n) {

		try {
			logger.info("{} - \n {}", msg, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n));
		} catch (JsonProcessingException e) {
			logger.warn("", e);
		}
	}
}
