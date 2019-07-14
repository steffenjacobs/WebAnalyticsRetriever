package me.steffenjacobs.webanalyticsretriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.steffenjacobs.webanalyticsretriever.domain.shared.SearchResults;

public class App {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	private GoogleSearchService googleService;
	private final RedditSearchResultService redditService = new RedditSearchResultService();

	private Map<String, SearchResults> getResultCounts(String... terms) {
		Map<String, SearchResults> result = new HashMap<>();
		for (String term : terms) {
			long googleResult = googleService.search(term);
			long redditResult = redditService.search(term);
			result.put(term, new SearchResults(term, googleResult, redditResult));
		}
		return result;
	}

	private ResourceBundle loadResources() {
		try (FileInputStream fis = new FileInputStream("./settings.properties")) {
			return new PropertyResourceBundle(fis);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void start(String... args) throws IOException {

		ResourceBundle rb = loadResources();
		String apiKey = rb.getString("google-api-key");

		googleService = new GoogleSearchService(apiKey);

		if (args.length != 1) {
			System.err.println("Invalid input. Please specify input file. Using default file ./terms.txt");
			args = new String[] { "terms.txt" };
		}
		File f = new File(args[0]);

		String csv = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

		StringBuilder sb = new StringBuilder();
		String[] split = csv.split("\r\n");
		Map<String, SearchResults> results = getResultCounts(split);
		results.forEach((k, v) -> {
			sb.append(k);
			sb.append(", ");
			sb.append(v.getGoogleSearchResultCount());
			sb.append(", ");
			sb.append(v.getRedditSearchResultCount());
			sb.append("\n");
		});

		FileUtils.write(new File("output.csv"), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Stored result to ./output.csv.");
	}

	public static void main(String[] args) throws IOException {
		new App().start();
	}
}
