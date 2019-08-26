
package me.steffenjacobs.webanalyticsretriever.domain.reddit;

import java.util.HashMap;
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
@JsonPropertyOrder({ "doc_count", "key" })
public class CreatedUtc {

	@JsonProperty("doc_count")
	private Long docCount;
	@JsonProperty("key")
	private Integer key;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("doc_count")
	public Long getDocCount() {
		return docCount;
	}

	@JsonProperty("doc_count")
	public void setDocCount(Long docCount) {
		this.docCount = docCount;
	}

	@JsonProperty("key")
	public Integer getKey() {
		return key;
	}

	@JsonProperty("key")
	public void setKey(Integer key) {
		this.key = key;
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
