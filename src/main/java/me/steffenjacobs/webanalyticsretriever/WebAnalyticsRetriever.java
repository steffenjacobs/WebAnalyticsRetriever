package me.steffenjacobs.webanalyticsretriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.steffenjacobs.webanalyticsretriever.domain.shared.SearchResults;

public class WebAnalyticsRetriever {
	private static final Logger LOG = LoggerFactory.getLogger(WebAnalyticsRetriever.class);

	private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH-mm");

	private GoogleSearchService googleService;
	private final RedditSearchResultService redditService = new RedditSearchResultService();
	private final GoogleSearchSeleniumService googleBrowserService = new GoogleSearchSeleniumService();

	private Collection<SearchResults> getResultCounts(String... terms) {
		Collection<SearchResults> result = new ArrayList<>();
		for (String term : terms) {
			long googleResult = googleService.search(term);
			long redditResult = redditService.search(term);
			long googleBrowserSearch = googleBrowserService.search(term);
			result.add(new SearchResults(term, googleResult, redditResult, googleBrowserSearch));
		}
		return result;
	}

	private ResourceBundle loadResource(String filename) {
		try (FileInputStream fis = new FileInputStream(filename)) {
			return new PropertyResourceBundle(fis);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void start(String... args) throws IOException {

		final String resourceFile = "./settings.properties";
		if (!new File(resourceFile).exists()) {
			LOG.warn("Configuration with google-api-key is missing. Please create {}.", resourceFile);
			return;
		}
		ResourceBundle rb = loadResource(resourceFile);
		String apiKey = rb.getString("google-api-key");

		googleService = new GoogleSearchService(apiKey);

		if (args.length != 1) {
			LOG.error("Invalid input. Please specify input file. Using default file ./terms.txt...");
			args = new String[] { "terms.txt" };
		}
		File f = new File(args[0]);

		String csv = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

		StringBuilder sb = new StringBuilder();
		String[] split = csv.split("\r\n");
		Collection<SearchResults> results = getResultCounts(split);
		results.forEach(v -> {
			sb.append(v.getTerm());
			sb.append(", ");
			sb.append(v.getGoogleSearchResultCount());
			sb.append(", ");
			sb.append(v.getRedditSearchResultCount());
			sb.append(", ");
			sb.append(v.getGoogleBrowserSearchResultCount());
			sb.append("\n");
		});

		final String filename = "output-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Stored result to ./{}", filename);
	}

	public static void main(String[] args) throws IOException {
		new WebAnalyticsRetriever().start();
	}
}
