package me.steffenjacobs.webanalyticsretriever.domain.shared;

/** @author Steffen Jacobs */
public class SearchResults {

	private final String term;
	private final long redditSearchResultCount;
	private final long googleSearchResultCount;

	public SearchResults(String term, long redditSearchResultCount, long googleSearchResultCount) {
		super();
		this.term = term;
		this.redditSearchResultCount = redditSearchResultCount;
		this.googleSearchResultCount = googleSearchResultCount;
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

}
