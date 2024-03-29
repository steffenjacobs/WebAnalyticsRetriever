
package me.steffenjacobs.webanalyticsretriever.domain.reddit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Part of the expected answer from the Reddit API when issuing a search request
 * over all comments.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "created_utc" })
public class Aggs {

	@JsonProperty("created_utc")
	private List<CreatedUtc> createdUtc = null;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("created_utc")
	public List<CreatedUtc> getCreatedUtc() {
		return createdUtc;
	}

	@JsonProperty("created_utc")
	public void setCreatedUtc(List<CreatedUtc> createdUtc) {
		this.createdUtc = createdUtc;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

}
