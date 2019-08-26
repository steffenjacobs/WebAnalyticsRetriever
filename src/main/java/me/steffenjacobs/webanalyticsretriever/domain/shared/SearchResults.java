package me.steffenjacobs.webanalyticsretriever.domain.shared;

/**
 * Represents a single run of the
 * {@link me.steffenjacobs.webanalyticsretriever.WebAnalyticsRetriever}. Can be
 * identified by the {@link term}. Contains search result counts for Reddit,
 * Google Search API, the Google WebSearch and the exact Google Web Search.
 * 
 * @author Steffen Jacobs
 */
public class SearchResults {

	private final String term;
	private final long redditSearchResultCount;
	private final long googleSearchResultCount;
	private final long googleBrowserSearchResultCount;
	private long googleBrowserExactSearchResultCount;

	public SearchResults(String term, long redditSearchResultCount, long googleSearchResultCount, long googleBrowserSearchResultCount, long googleBrowserExactSearchResultCount) {
		super();
		this.term = term;
		this.redditSearchResultCount = redditSearchResultCount;
		this.googleSearchResultCount = googleSearchResultCount;
		this.googleBrowserSearchResultCount = googleBrowserSearchResultCount;
		this.googleBrowserExactSearchResultCount = googleBrowserExactSearchResultCount;
	}

	public String getTerm() {
		return term;
	}

	public long getRedditSearchResultCount() {
		return redditSearchResultCount;
	}

	public long getGoogleSearchResultCount() {
		return googleSearchResultCount;
	}

	public long getGoogleBrowserSearchResultCount() {
		return googleBrowserSearchResultCount;
	}

	public long getGoogleBrowserExactSearchResultCount() {
		return googleBrowserExactSearchResultCount;
	}

}
