package me.steffenjacobs.webanalyticsretriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.steffenjacobs.webanalyticsretriever.domain.shared.SearchResults;

/**
 * This class contains the main entry point for the web mining application that
 * collects the search results from the Google WebSearch, the Google Search API
 * and the Reddit Search.
 */
public class WebAnalyticsRetriever {
	private static final Logger LOG = LoggerFactory.getLogger(WebAnalyticsRetriever.class);

	private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH-mm");

	private GoogleSearchApiService googleSearchApiService;
	private final RedditSearchResultService redditService = new RedditSearchResultService();
	private final GoogleSearchSeleniumService googleBrowserService = new GoogleSearchSeleniumService();

	public static final String[] KEY_WORDS = new String[] { "IoT", "Home Automation", "Smart Home" };

	/** Main entry point of the application; Starts a new instance of this class. */
	public static void main(String[] args) throws IOException {
		new WebAnalyticsRetriever().start();
	}

	/** Starts a new instance of this class. */
	public void start(String... args) throws IOException {

		// load the google search api key from the settings.properties
		final String resourceFile = "./settings.properties";
		if (!new File(resourceFile).exists()) {
			LOG.error("Configuration with google-api-key is missing. Please create {}.", resourceFile);
			return;
		}
		ResourceBundle rb = loadResource(resourceFile);
		String apiKey = rb.getString("google-api-key");

		googleSearchApiService = new GoogleSearchApiService(apiKey);

		// load the terms.txt file with the platform names in it
		if (args.length != 1) {
			LOG.warn("Invalid input. Please specify input file. Using default file ./terms.txt...");
			args = new String[] { "terms.txt" };
		}
		File f = new File(args[0]);

		String csv = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

		String[] split = csv.split("\r\n");

		List<String> terms = new ArrayList<>();
		terms.addAll(Arrays.asList(split));

		// extend each platform name with all marker permutations
		for (String term : split) {
			for (String keyWord : KEY_WORDS) {
				terms.add(term + " " + keyWord);
			}
		}

		// shuffle the list of turns to make sure the google search api limitation of
		// 100 requests per day does not result in only the first 100 terms having
		// actual results
		Collections.shuffle(terms);

		final String filename = "output-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		final File file = new File(filename);
		LOG.info("Storing result to ./{}...", filename);

		// retrieve the results and store the results directly to csv term-wise
		getResultCounts(terms, v -> {
			final StringBuilder sb = new StringBuilder();
			sb.append(v.getTerm());
			sb.append(", ");
			sb.append(v.getGoogleSearchResultCount());
			sb.append(", ");
			sb.append(v.getRedditSearchResultCount());
			sb.append(", ");
			sb.append(v.getGoogleBrowserSearchResultCount());
			sb.append(", ");
			sb.append(v.getGoogleBrowserExactSearchResultCount());
			sb.append("\n");
			try {
				FileUtils.write(file, sb.toString(), StandardCharsets.UTF_8, true);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		});

	}

	/**
	 * Retrieves the actual search result counts for each term in {@link Collection
	 * terms} from each search engine and delivers the result to the given
	 * {@link Consumer consumer}.
	 */
	private void getResultCounts(Collection<String> terms, Consumer<SearchResults> consumer) {
		int count = 0;
		for (String term : terms) {
			// retrieve results
			CompletableFuture<Long> googleResultFuture = CompletableFuture.supplyAsync(() -> googleSearchApiService.search(term));
			CompletableFuture<Long> redditResultFuture = CompletableFuture.supplyAsync(() -> redditService.search(term));
			CompletableFuture<Long> googleBrowserSearchResultFuture = CompletableFuture.supplyAsync(() -> googleBrowserService.search(term));
			CompletableFuture<Long> googleBrowserSearchExactResultFuture = null;

			// handle exact search for search terms with whitespaces
			final boolean containsWhitespaces = term.contains(" ");
			if (containsWhitespaces) {
				googleBrowserSearchExactResultFuture = CompletableFuture.supplyAsync(() -> googleBrowserService.search(term.replace(" ", " AND ")));
			}

			// wait until all 4 (or 3) results are back
			long googleSearchApiResult = googleResultFuture.join();
			long redditResult = redditResultFuture.join();
			long googleBrowserSearchResult = googleBrowserSearchResultFuture.join();
			long googleBrowserExactSearchResult = googleBrowserSearchResult;

			if (containsWhitespaces) {
				googleBrowserExactSearchResult = googleBrowserSearchExactResultFuture.join();
			}

			// give the result to the consumer
			consumer.accept(new SearchResults(term, googleSearchApiResult, redditResult, googleBrowserSearchResult,
					containsWhitespaces ? googleBrowserExactSearchResult : googleBrowserSearchResult));

			// progress indicator
			count++;
			LOG.info("Retrieved results for search term {} ({}/{})", term, count, terms.size());
		}
	}

	/** Loads a resource bundle from a file. */
	private ResourceBundle loadResource(String filename) {
		try (FileInputStream fis = new FileInputStream(filename)) {
			return new PropertyResourceBundle(fis);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
