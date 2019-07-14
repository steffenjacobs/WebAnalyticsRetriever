package me.steffenjacobs.webanalyticsretriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.steffenjacobs.webanalyticsretriever.domain.shared.SearchResults;

public class AppAsync {
	private static final Logger LOG = LoggerFactory.getLogger(AppAsync.class);

	private GoogleSearchService googleService;
	private final RedditSearchResultService redditService = new RedditSearchResultService();

	private List<SearchResults> getResultCounts(String... terms) {
		final List<CompletableFuture<SearchResults>> futures = Arrays.stream(terms).map(term -> doAsync(() -> {
			long googleResult = googleService.search(term);
			long redditResult = redditService.search(term);
			return new SearchResults(term, redditResult, googleResult);
		})).collect(Collectors.toList());

		return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
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
			LOG.error("Configuration with google-api-key is missing. Please create {}.", resourceFile);
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
		List<SearchResults> results = getResultCounts(split);
		results.forEach(v -> {
			sb.append(v.getTerm());
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
		new AppAsync().start();
	}

	private <T> CompletableFuture<T> doAsync(CallableWithoutException<T> toCall) {
		return CompletableFuture.supplyAsync(new Supplier<T>() {
			@Override
			public T get() {
				return toCall.call();
			}
		});
	}

	static interface CallableWithoutException<T> {
		T call();
	}
}
