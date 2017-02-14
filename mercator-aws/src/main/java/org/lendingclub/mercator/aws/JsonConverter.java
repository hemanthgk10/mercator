package org.lendingclub.mercator.aws;

import java.util.Optional;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

public class JsonConverter {

	static ObjectMapper mapper = new ObjectMapper();
	
	Region region;
	String account;
	
	private JsonConverter() {
		// TODO Auto-generated constructor stub
	}

	public static JsonConverter newInstance(String account, Regions region) {
		return newInstance(account,Region.getRegion(region));
	}
	public static JsonConverter newInstance(String account, Region region) {
		JsonConverter converter = new JsonConverter();
		converter.account = account;
		converter.region = region;
		return converter;
		
	}
	public ObjectNode toJson(Object x, String arn) {
		ObjectNode n = mapper.valueToTree(x);
		n.put("region", region.getName());
		n.put("account", account);
		n = flatten(n, arn);
		return n;
	}

	protected ObjectNode flatten(ObjectNode n, String arn) {
		ObjectNode r = mapper.createObjectNode();

		n.fields().forEachRemaining(it -> {

			if (!it.getValue().isContainerNode()) {
				r.set("aws_" + it.getKey(), it.getValue());
			}

		});

		n.path("tags").iterator().forEachRemaining(it -> {
			String tagKey = "aws_tag_" + it.path("key").asText();
			r.put(tagKey, it.path("value").asText());
		});

	
		if (!Strings.isNullOrEmpty(arn)) {
			r.put("aws_arn", arn);
		}

		return r;
	}
}
