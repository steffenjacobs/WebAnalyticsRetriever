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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Steffen Jacobs */
public class AnalyticsAggregator {
	private static final Logger LOG = LoggerFactory.getLogger(AnalyticsAggregator.class);

	private static final Pattern FILE_PATTERN = Pattern.compile("output-\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d.csv");
	private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd-HH-mm");

	public static void main(String[] args) throws IOException {

		final Map<String, Collection<Long>> results = new HashMap<>();

		int countEntries = 0;
		int countFiles = 0;
		for (File file : new File(".").listFiles()) {
			Matcher m = FILE_PATTERN.matcher(file.getName());
			if (m.find()) {
				countFiles++;
				for (String line : Files.readAllLines(file.toPath())) {
					countEntries++;
					String[] split = line.split(",");

					long val = Long.parseLong(split[1].trim());
					if (val == -1) {
						continue;
					}
					results.putIfAbsent(split[0], new ArrayList<Long>());
					results.get(split[0]).add(val);

				}
			}
		}
		LOG.info("Imported {} entries from {} files.", countEntries, countFiles);

		Set<Result> transformedResults = new HashSet<>();

		for (Map.Entry<String, Collection<Long>> e : results.entrySet()) {
			double avg = e.getValue().stream().mapToLong(Long::longValue).average().orElse(-1);
			transformedResults.add(new Result(e.getKey().trim(), avg, e.getValue().size()));
		}

		LOG.info("Aggregated {} values to {} vlaues.", countEntries, transformedResults.size());

		exportToFile(transformedResults, false);
		exportToFile(transformedResults, true);
	}

	private static void exportToFile(Set<Result> transformedResults, final boolean rounded) throws IOException {
		final StringBuilder sb = new StringBuilder("name,average,count\n");
		for (Result r : transformedResults) {
			sb.append(r.getName());
			sb.append(",");
			sb.append(new BigDecimal(rounded ? Math.round(r.getAverage()) : r.getAverage()).toPlainString());
			sb.append(",");
			sb.append(r.getCount());
			sb.append("\n");
		}

		final String filename = "output-" + (rounded ? "rounded" : "exact") + "-aggregated-" + sdf.format(Calendar.getInstance().getTime()) + ".csv";
		FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		LOG.info("Exported {} aggregated values to file '{}'.", rounded ? "rounded" : "exact", filename);
	}

	static class Result {
		private final String name;
		private final double average;
		private final int count;

		public Result(String name, double average, int count) {
			super();
			this.name = name;
			this.average = average;
			this.count = count;
		}

		public String getName() {
			return name;
		}

		public double getAverage() {
			return average;
		}

		public int getCount() {
			return count;
		}

	}

}
