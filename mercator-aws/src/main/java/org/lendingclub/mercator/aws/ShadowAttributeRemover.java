package org.lendingclub.mercator.aws;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import io.macgyver.neorx.rest.NeoRxClient;

public class ShadowAttributeRemover {

	Logger logger = LoggerFactory.getLogger(ShadowAttributeRemover.class);
	
	NeoRxClient neo4j;
	
	public ShadowAttributeRemover(NeoRxClient client) {
		this.neo4j = client;
	}
	
	
	class SanitizationFilter implements Predicate<String> {

		boolean isValidChar(char c) {
			return c==':' || Character.isJavaIdentifierPart(c);
		}
		@Override
		public boolean test(String t) {
			for (char c: t.toCharArray()) {
				if (!isValidChar(c)) {
					return false;
				}
			}
			return true;
		}
		
	}
	public void removeTagAttributes(String label, JsonNode desired, JsonNode cache) {
		removeAttributes(label, desired, cache, new Predicate<String>() {

			@Override
			public boolean test(String t) {
				return t.startsWith("aws_tag_");
			}
			
		});
	}
	public void removeAttributes(String label,JsonNode desired, JsonNode cache, Predicate<String> predicate) {
		List<String> attrs = getAttributesToRemove(desired, cache,predicate);
		

			if (!attrs.isEmpty()) {
				List<String> fragments = Lists.newArrayList();
				attrs.stream().filter(new SanitizationFilter()).forEach(n -> {
					fragments.add("x." + n);
				});
				String clause = Joiner.on(", ").join(fragments);
				String cypher = "match (x:"+label+" {aws_arn:{aws_arn}}) remove " + clause + " return x";
				neo4j.execCypher(cypher, "aws_arn", desired.get("aws_arn").asText());
			}


	}

	protected List<String> getAttributesToRemove(JsonNode actual, JsonNode neo4j, Predicate<String> predicate) {
		List<String> list = Lists.newArrayList();
		neo4j.fieldNames().forEachRemaining(n -> {
			if (n != null) {

				if (predicate.test(n)) {
					if (!actual.has(n)) {
						list.add(n);
					}

				}
			}
		});

		return list;
	}
}
