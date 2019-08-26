package me.steffenjacobs.webanalyticsretriever;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.steffenjacobs.webanalyticsretriever.domain.google.GoogleCustomSearchTotalResult;

/**
 * Simple Wrapper service to call the Google Search API:
 * <a href="https://developers.google.com/custom-search/v1/overview">
 * https://developers.google.com/custom-search/v1/overview</a>
 * 
 * @author Steffen Jacobs
 */
public class GoogleSearchApiService {

	private static final Logger LOG = LoggerFactory.getLogger(GoogleSearchApiService.class);

	private final String apiKey;

	public GoogleSearchApiService(String apiKey) {
		this.apiKey = apiKey;
	}

	/** Search for the given {@link String term} via the Google Search Api */
	public long search(String term) {
		final ObjectMapper objectMapper = new ObjectMapper();
		try {
			// URL encoding
			final String encodedTerm = URLEncoder.encode(term, "UTF-8");

			// send GET and automatically unpack it via Jackson
			GoogleCustomSearchTotalResult result = objectMapper.readValue(new URL("https://www.googleapis.com/customsearch/v1?key=" + apiKey
					+ "&cx=002845322276752338984:vxqzfa86nqc&q=" + encodedTerm + "&exactTerms=" + encodedTerm + "&alt=json&fields=queries(request(totalResults))"),
					new TypeReference<GoogleCustomSearchTotalResult>() {
					});
			LOG.info("Retrieved Google Search API result for '{}'.", term);
			return Long.parseLong(result.getQueries().getRequest().get(0).getTotalResults());
		} catch (IOException e) {
			if (e.getMessage().contains("403")) {
				LOG.error("Daily limit exceeded.");
			} else {
				LOG.error(e.getMessage(), e);
			}
		}
		return -1;
	}
}
