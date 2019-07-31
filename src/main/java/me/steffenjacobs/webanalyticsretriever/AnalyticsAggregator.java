package me.steffenjacobs.webanalyticsretriever;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.steffenjacobs.webanalyticsretriever.domain.shared.SearchResults;

/** @author Steffen Jacobs */
public class AnalyticsAggregator {
	private static final Logger LOG = LoggerFactory.getLogger(AnalyticsAggregator.class);

	private static final Pattern FILE_PATTERN = Pattern.compile("output-\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d.csv");
	private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH-mm");

	public static void main(String[] args) throws IOException {

		final Map<String, Collection<SearchResults>> results = new HashMap<>();

		int countEntries = 0;
		int countFiles = 0;
		for (File file : new File(".").listFiles()) {
			Matcher m = FILE_PATTERN.matcher(file.getName());
			if (m.find()) {
				countFiles++;
				for (String line : Files.readAllLines(file.toPath())) {
					countEntries++;
					String[] split = line.split(",");

					long valGoogle = -1;
					long valReddit = -1;
					long valGoogleBrowser = -1;
					if (split.length == 2) {
						valGoogle = Long.parseLong(split[1].trim());
					} else if (split.length == 3) {
						valReddit = Long.parseLong(split[1].trim());
						valGoogle = Long.parseLong(split[2].trim());
					} else {
						valReddit = Long.parseLong(split[1].trim());
						valGoogle = Long.parseLong(split[2].trim());
						valGoogleBrowser = Long.parseLong(split[3].trim());
					}
					results.putIfAbsent(split[0], new ArrayList<SearchResults>());
					results.get(split[0]).add(new SearchResults(split[0], valReddit, valGoogle, valGoogleBrowser));

				}
			}
		}
		LOG.info("Imported {} entries from {} files.", countEntries, countFiles);

		final Set<Result> transformedResults = new HashSet<>();

		for (Map.Entry<String, Collection<SearchResults>> e : results.entrySet()) {
			LongStream sGoogle = filteredStream(e.getValue(), SearchResults::getGoogleSearchResultCount);
			final long countGoogle = sGoogle.count();

			sGoogle = filteredStream(e.getValue(), SearchResults::getGoogleSearchResultCount);
			final double avgGoogle = sGoogle.average().orElse(-1);

			LongStream sReddit = filteredStream(e.getValue(), SearchResults::getRedditSearchResultCount);
			final long countReddit = sReddit.count();

			sReddit = filteredStream(e.getValue(), SearchResults::getRedditSearchResultCount);
			final double avgReddit = sReddit.average().orElse(-1);

			LongStream sGoogleWebSearch = filteredStream(e.getValue(), SearchResults::getGoogleBrowserSearchResultCount);
			final double avgGoogleWebSearch = sGoogleWebSearch.average().orElse(-1);

			sGoogleWebSearch = filteredStream(e.getValue(), SearchResults::getGoogleBrowserSearchResultCount);
			final long countGoogleWebSearch = sGoogleWebSearch.count();

			transformedResults.add(new Result(e.getKey().trim(), avgGoogle, avgReddit, avgGoogleWebSearch, countGoogle, countReddit, countGoogleWebSearch));
		}

		LOG.info("Aggregated {} values to {} vlaues.", countEntries, transformedResults.size());

		exportToFile(transformedResults, false);
		exportToFile(transformedResults, true);
		exportToFileWithoutCountRounded(transformedResults);
	}

	private static LongStream filteredStream(Collection<SearchResults> l, Function<SearchResults, Long> mapper) {
		return l.stream().mapToLong(v -> mapper.apply(v).longValue()).filter(v -> v != -1);
	}

	private static void exportToFile(Set<Result> transformedResults, final boolean rounded) throws IOException {
		final StringBuilder sb = new StringBuilder("name,averageGoogle,averageReddit,averageGoogleSearchBrowser,countGoogle,countReddit, countGoogleSearchBrowser\n");
		for (Result r : transformedResults) {
			sb.append(r.getName());
			sb.append(",");
			sb.append(new BigDecimal(rounded ? Math.round(r.getAverageGoogle()) : r.getAverageGoogle()).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(rounded ? Math.round(r.getAverageReddit()) : r.getAverageReddit()).toPlainString());
			sb.append(",");
			sb.append(r.getCountGoogle());
			sb.append(",");
			sb.append(r.getCountReddit());
			sb.append("\n");
		}

		final String filename = "output-" + (rounded ? "rounded" : "exact") + "-aggregated-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Exported {} aggregated values to file '{}'.", rounded ? "rounded" : "exact", filename);
	}

	private static void exportToFileWithoutCountRounded(Set<Result> transformedResults) throws IOException {
		final StringBuilder sb = new StringBuilder("name,averageGoogle,averageReddit\n");
		for (Result r : transformedResults) {
			sb.append(r.getName());
			sb.append(",");
			sb.append(new BigDecimal(Math.round(r.getAverageGoogle())).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(Math.round(r.getAverageReddit())).toPlainString());
			sb.append("\n");
		}

		final String filename = "output-woresultsrounded-aggregated-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Exported rounded and aggregated values withour result to file '{}'.", filename);
	}

	static class Result {
		private final String name;
		private final double averageGoogle;
		private final long countGoogle;
		private final long countReddit;
		private final double averageReddit;
		private final long countGoogleWebSearch;
		private final double averageGoogleWebSearch;

		public Result(String name, double averageGoogle, double averageReddit, double averageGoogleWebSearch, long countGoogle, long countReddit, long countGoogleWebSearch) {
			super();
			this.name = name;
			this.averageGoogle = averageGoogle;
			this.averageReddit = averageReddit;
			this.averageGoogleWebSearch = averageGoogleWebSearch;
			this.countGoogle = countGoogle;
			this.countReddit = countReddit;
			this.countGoogleWebSearch = countGoogleWebSearch;
		}

		public String getName() {
			return name;
		}

		public double getAverageGoogle() {
			return averageGoogle;
		}

		public double getAverageReddit() {
			return averageReddit;
		}

		public long getCountGoogle() {
			return countGoogle;
		}

		public long getCountReddit() {
			return countReddit;
		}

		public double getAverageGoogleWebSearch() {
			return averageGoogleWebSearch;
		}

		public long getCountGoogleWebSearch() {
			return countGoogleWebSearch;
		}
	}
}
