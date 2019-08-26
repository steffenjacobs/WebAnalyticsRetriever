package me.steffenjacobs.webanalyticsretriever;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.steffenjacobs.webanalyticsretriever.domain.reddit.RedditSearchResult;

/**
 * Simple wrapper to call the Reddit Comment API <a href=
 * "https://api.pushshift.io/reddit/search/comment/">https://api.pushshift.io/reddit/search/comment/</a>.
 * 
 * @author Steffen Jacobs
 */
public class RedditSearchResultService {
	private static final Logger LOG = LoggerFactory.getLogger(RedditSearchResultService.class);

	/** Searches for the {@link String term} in the Reddit comments */
	public long search(String term) {
		final ObjectMapper objectMapper = new ObjectMapper();
		try {
			// URL encode
			final String encodedTerm = URLEncoder.encode(term, "UTF-8");

			// send GET and automatically unpack it with Jackson
			RedditSearchResult result = objectMapper.readValue(
					new URL("https://api.pushshift.io/reddit/search/comment/?q=" + encodedTerm + "&aggs=created_utc&frequency=year&size=0"),
					new TypeReference<RedditSearchResult>() {
					});
			LOG.info("Retrieved Reddit search result for '{}'.", term);
			return result.getAggs().getCreatedUtc().stream().mapToLong(c -> c.getDocCount()).sum();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return -1;
	}

}
