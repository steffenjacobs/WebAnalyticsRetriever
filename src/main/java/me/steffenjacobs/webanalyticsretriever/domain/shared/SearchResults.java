package me.steffenjacobs.webanalyticsretriever.domain.shared;

/** @author Steffen Jacobs */
public class SearchResults {

	private final String term;
	private final long redditSearchResultCount;
	private final long googleSearchResultCount;
	private final long googleBrowserSearchResultCount;

	public SearchResults(String term, long redditSearchResultCount, long googleSearchResultCount, long googleBrowserSearchResultCount) {
		super();
		this.term = term;
		this.redditSearchResultCount = redditSearchResultCount;
		this.googleSearchResultCount = googleSearchResultCount;
		this.googleBrowserSearchResultCount = googleBrowserSearchResultCount;
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

}
