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

/** @author Steffen Jacobs */
public class AnalyticsAggregator {
	private static final Logger LOG = LoggerFactory.getLogger(AnalyticsAggregator.class);

	private static final Pattern FILE_PATTERN = Pattern.compile("output-\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d.csv");
	private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH-mm");

	public static void main(String[] args) throws IOException {

		final Map<String, Collection<Pair<Long>>> results = new HashMap<>();

		int countEntries = 0;
		int countFiles = 0;
		for (File file : new File(".").listFiles()) {
			Matcher m = FILE_PATTERN.matcher(file.getName());
			if (m.find()) {
				countFiles++;
				for (String line : Files.readAllLines(file.toPath())) {
					countEntries++;
					String[] split = line.split(",");

					long valGoogle = Long.parseLong(split[1].trim());
					long valReddit = -1;
					if (split.length > 2) {
						valReddit = Long.parseLong(split[2].trim());
					}
					results.putIfAbsent(split[0], new ArrayList<Pair<Long>>());
					results.get(split[0]).add(new Pair<>(valGoogle, valReddit));

				}
			}
		}
		LOG.info("Imported {} entries from {} files.", countEntries, countFiles);

		final Set<Result> transformedResults = new HashSet<>();

		for (Map.Entry<String, Collection<Pair<Long>>> e : results.entrySet()) {
			LongStream sGoogle = filteredStream(e.getValue(), Pair::getA);
			final long countGoogle = sGoogle.count();

			sGoogle = filteredStream(e.getValue(), Pair::getA);
			final double avgGoogle = sGoogle.average().orElse(-1);

			LongStream sReddit = filteredStream(e.getValue(), Pair::getB);
			final long countReddit = sReddit.count();

			sReddit = filteredStream(e.getValue(), Pair::getB);
			final double avgReddit = sReddit.average().orElse(-1);

			transformedResults.add(new Result(e.getKey().trim(), avgGoogle, avgReddit, (int) countGoogle, (int) countReddit));
		}

		LOG.info("Aggregated {} values to {} vlaues.", countEntries, transformedResults.size());

		exportToFile(transformedResults, false);
		exportToFile(transformedResults, true);
		exportToFileWithoutCountRounded(transformedResults);
	}

	private static LongStream filteredStream(Collection<Pair<Long>> l, Function<Pair<Long>, Long> mapper) {
		return l.stream().mapToLong(v -> mapper.apply(v).longValue()).filter(v -> v != -1);
	}

	private static void exportToFile(Set<Result> transformedResults, final boolean rounded) throws IOException {
		final StringBuilder sb = new StringBuilder("name,averageGoogle,averageReddit,countGoogle,countReddit\n");
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
		private final int countGoogle;
		private final int countReddit;
		private final double averageReddit;

		public Result(String name, double averageGoogle, double averageReddit, int countGoogle, int countReddit) {
			super();
			this.name = name;
			this.averageGoogle = averageGoogle;
			this.averageReddit = averageReddit;
			this.countGoogle = countGoogle;
			this.countReddit = countReddit;
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

		public int getCountGoogle() {
			return countGoogle;
		}

		public int getCountReddit() {
			return countReddit;
		}
	}

	static class Pair<T> {
		private final T a, b;

		public Pair(T a, T b) {
			super();
			this.a = a;
			this.b = b;
		}

		public T getA() {
			return a;
		}

		public T getB() {
			return b;
		}

	}

}
