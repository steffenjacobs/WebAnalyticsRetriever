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
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.MathArrays;
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
					String[] split = line.split(",");
					countEntries += split.length;

					long valGoogle = -1;
					long valReddit = -1;
					long valGoogleBrowser = -1;
					long valGoogleBrowserExact = -1;
					if (split.length == 2) {
						valGoogle = Long.parseLong(split[1].trim());
					} else if (split.length == 3) {
						valReddit = Long.parseLong(split[1].trim());
						valGoogle = Long.parseLong(split[2].trim());
					} else if (split.length == 4) {
						valReddit = Long.parseLong(split[1].trim());
						valGoogle = Long.parseLong(split[2].trim());
						valGoogleBrowser = Long.parseLong(split[3].trim());
					} else {
						valReddit = Long.parseLong(split[1].trim());
						valGoogle = Long.parseLong(split[2].trim());
						valGoogleBrowser = Long.parseLong(split[3].trim());
						valGoogleBrowserExact = Long.parseLong(split[4].trim());
					}
					results.putIfAbsent(split[0], new ArrayList<SearchResults>());
					results.get(split[0]).add(new SearchResults(split[0], valReddit, valGoogle, valGoogleBrowser, valGoogleBrowserExact));

				}
			}
		}
		LOG.info("Imported {} entries from {} files.", countEntries, countFiles);

		final Map<String, Result> transformedResults = new HashMap<>();

		// filter null values (-1) and calculate average for each entry
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

			LongStream sGoogleWebSearchExact = filteredStream(e.getValue(), SearchResults::getGoogleBrowserExactSearchResultCount);
			final double avgGoogleWebSearchExact = sGoogleWebSearchExact.average().orElse(-1);

			sGoogleWebSearchExact = filteredStream(e.getValue(), SearchResults::getGoogleBrowserExactSearchResultCount);
			final long countGoogleWebSearchExact = sGoogleWebSearchExact.count();

			transformedResults.put(e.getKey().trim(), new Result(e.getKey().trim(), avgGoogle, avgReddit, avgGoogleWebSearch, avgGoogleWebSearchExact, countGoogle, countReddit,
					countGoogleWebSearch, countGoogleWebSearchExact));
		}

		LOG.info("Aggregated {} entries to {} entries.", countEntries, transformedResults.size());

		// consolidate key words
		final Set<Result> consolidatedResults = new HashSet<>();

		for (String platformName : Files.readAllLines(new File("./terms.txt").toPath())) {

			Set<Result> keywordBasedResults = new HashSet<>();
			for (String keyWord : WebAnalyticsRetriever.KEY_WORDS) {
				keywordBasedResults.add(transformedResults.get(platformName + " " + keyWord));
			}

			double avgGoogle = 0;
			double avgGoogleWeb = 0;
			double avgGoogleWebExact = 0;
			double avgReddit = 0;

			long cntGoogle = 0;
			long cntGoogleWeb = 0;
			long cntGoogleWebExact = 0;
			long cntReddit = 0;

			for (Result r : keywordBasedResults) {
				avgGoogle += r.getAverageGoogle();
				avgGoogleWeb += r.getAverageGoogleWebSearch();
				avgGoogleWebExact += r.getAverageGoogleWebSearchExact();
				avgReddit += r.getAverageReddit();

				cntGoogle += r.getCountGoogle();
				cntGoogleWeb += r.getCountGoogleWebSearch();
				cntGoogleWebExact += r.getCountGoogleWebSearchExact();
				cntReddit += r.getCountReddit();
			}
			consolidatedResults.add(new Result(platformName, avgGoogle / WebAnalyticsRetriever.KEY_WORDS.length, avgReddit / WebAnalyticsRetriever.KEY_WORDS.length,
					avgGoogleWeb / WebAnalyticsRetriever.KEY_WORDS.length, avgGoogleWebExact / WebAnalyticsRetriever.KEY_WORDS.length, cntGoogle, cntReddit, cntGoogleWeb,
					cntGoogleWebExact));
		}

		// store results
		exportToFile(consolidatedResults, false);
		exportToFile(consolidatedResults, true);
		exportToFileWithoutCountRounded(consolidatedResults);
		exportToTableFile(consolidatedResults);

		storeStd(results);
	}

	private static void storeStd(final Map<String, Collection<SearchResults>> results) throws IOException {
		final StringBuilder sb = new StringBuilder("name,stdGoogle,stdReddit,stdGoogleSearchBrowser,stdGoogleSearchBrowserExact\n");
		final StandardDeviation sd = new StandardDeviation();
		for (final Map.Entry<String, Collection<SearchResults>> e : results.entrySet()) {
			LongStream sGoogle = filteredStream(e.getValue(), SearchResults::getGoogleSearchResultCount);
			long sumGoogle = filteredStream(e.getValue(), SearchResults::getGoogleSearchResultCount).sum();
			final double sdGoogle = sumGoogle > 1 ? sd.evaluate(MathArrays.normalizeArray(sGoogle.mapToDouble(d -> d).toArray(), 1)) : 0;

			LongStream sReddit = filteredStream(e.getValue(), SearchResults::getRedditSearchResultCount);
			long sumReddit = filteredStream(e.getValue(), SearchResults::getRedditSearchResultCount).sum();
			final double sdReddit = sumReddit > 1 ? sd.evaluate(MathArrays.normalizeArray(sReddit.mapToDouble(d -> d).toArray(), 1)) : 0;

			LongStream sGWS = filteredStream(e.getValue(), SearchResults::getGoogleBrowserSearchResultCount);
			long sumGWS = filteredStream(e.getValue(), SearchResults::getGoogleBrowserSearchResultCount).sum();
			final double sdGWS = sumGWS > 1 ? sd.evaluate(MathArrays.normalizeArray(sGWS.mapToDouble(d -> d).toArray(), 1)) : 0;

			LongStream sGWSE = filteredStream(e.getValue(), SearchResults::getGoogleBrowserExactSearchResultCount);
			long sumGWSE = filteredStream(e.getValue(), SearchResults::getGoogleBrowserExactSearchResultCount).sum();
			final double sdGWSE = sumGWSE > 1 ? sd.evaluate(MathArrays.normalizeArray(sGWSE.mapToDouble(d -> d).toArray(), 1)) : 0;

			sb.append(e.getKey());
			sb.append(",");
			sb.append(sdGoogle);
			sb.append(",");
			sb.append(sdReddit);
			sb.append(",");
			sb.append(sdGWS);
			sb.append(",");
			sb.append(sdGWSE);
			sb.append("\n");
		}

		final String filename = "output-std-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Exported standard deviation values to file '{}'.", filename);
	}

	private static LongStream filteredStream(Collection<SearchResults> l, Function<SearchResults, Long> mapper) {
		return l.stream().mapToLong(v -> mapper.apply(v).longValue()).filter(v -> v != -1);
	}

	private static void exportToFile(Set<Result> transformedResults, final boolean rounded) throws IOException {
		final StringBuilder sb = new StringBuilder(
				"name,averageGoogle,averageReddit,averageGoogleSearchBrowser,averageGoogleSearchBrowserExact,countGoogle,countReddit,countGoogleSearchBrowser,countGoogleSearchBrowserExact\n");
		for (Result r : transformedResults) {
			sb.append(r.getName());
			sb.append(",");
			sb.append(new BigDecimal(rounded ? Math.round(r.getAverageGoogle()) : r.getAverageGoogle()).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(rounded ? Math.round(r.getAverageReddit()) : r.getAverageReddit()).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(rounded ? Math.round(r.getAverageGoogleWebSearch()) : r.getAverageGoogleWebSearch()).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(rounded ? Math.round(r.getAverageGoogleWebSearchExact()) : r.getAverageGoogleWebSearchExact()).toPlainString());
			sb.append(",");
			sb.append(r.getCountGoogle());
			sb.append(",");
			sb.append(r.getCountReddit());
			sb.append(",");
			sb.append(r.getCountGoogleWebSearch());
			sb.append(",");
			sb.append(r.getCountGoogleWebSearchExact());
			sb.append("\n");
		}

		final String filename = "output-consolidated-" + (rounded ? "rounded" : "exact") + "-aggregated-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Exported {} aggregated values to file '{}'.", rounded ? "rounded" : "exact", filename);
	}

	private static void exportToFileWithoutCountRounded(Set<Result> transformedResults) throws IOException {
		final StringBuilder sb = new StringBuilder("name,averageGoogle,averageReddit,averageGoogleSearchBrowser,averageGoogleSearchBrowserExact\n");
		for (Result r : transformedResults) {
			sb.append(r.getName());
			sb.append(",");
			sb.append(new BigDecimal(Math.round(r.getAverageGoogle())).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(Math.round(r.getAverageReddit())).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(Math.round(r.getAverageGoogleWebSearch())).toPlainString());
			sb.append(",");
			sb.append(new BigDecimal(Math.round(r.getAverageGoogleWebSearchExact())).toPlainString());
			sb.append("\n");
		}

		final String filename = "output-woresults-consolidated-rounded-aggregated-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Exported table to file '{}'.", filename);
	}

	private static void exportToTableFile(Set<Result> transformedResults) throws IOException {
		StringBuilder sb = new StringBuilder();
		// sb.append("\\begin{tabular}{l|c|c|c|c}\r\n" );
		// sb.append("\\textbf{Name of the} & \\textbf{\\# Search Results} &
		// \\textbf{\\# Comments on} & \\textbf{\\# Search Results on} & \\textbf{\\#
		// Search Results on exact}\\\\\r\n" +
		// " \\textbf{IoT Platform} & \\textbf{on Google API} & \\textbf{Reddit} &
		// \\textbf{Google Web Search} & \\textbf{Google Web Search}\\\\\r\n");
		// sb.append(" \\hline\r\n");
		for (Result r : transformedResults) {
			sb.append(r.getName());
			sb.append(" & ");
			sb.append(new BigDecimal(Math.round(r.getAverageGoogle())).toPlainString());
			sb.append(" & ");
			sb.append(new BigDecimal(Math.round(r.getAverageReddit())).toPlainString());
			sb.append(" & ");
			sb.append(new BigDecimal(Math.round(r.getAverageGoogleWebSearch())).toPlainString());
			sb.append(" & ");
			sb.append(new BigDecimal(Math.round(r.getAverageGoogleWebSearchExact())).toPlainString());
			sb.append(" \\\\\n");
		}
		// sb.append(" \\end{tabular}");

		final String filename = "output-results-table-" + sdf.format(Calendar.getInstance().getTime()) + ".txt";
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
		private final long countGoogleWebSearchExact;
		private final double averageGoogleWebSearchExact;

		public Result(String name, double averageGoogle, double averageReddit, double averageGoogleWebSearch, double averageGoogleWebSearchExact, long countGoogle,
				long countReddit, long countGoogleWebSearch, long countGoogleWebSearchExact) {
			super();
			this.name = name;
			this.averageGoogle = averageGoogle;
			this.averageReddit = averageReddit;
			this.averageGoogleWebSearch = averageGoogleWebSearch;
			this.averageGoogleWebSearchExact = averageGoogleWebSearchExact;
			this.countGoogle = countGoogle;
			this.countReddit = countReddit;
			this.countGoogleWebSearch = countGoogleWebSearch;
			this.countGoogleWebSearchExact = countGoogleWebSearchExact;
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

		public double getAverageGoogleWebSearchExact() {
			return averageGoogleWebSearchExact;
		}

		public long getCountGoogleWebSearchExact() {
			return countGoogleWebSearchExact;
		}
	}
}
