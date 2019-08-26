package me.steffenjacobs.webanalyticsretriever;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts the google search result count from the Google Web Search via
 * <a href="https://www.seleniumhq.org/">Selenium</a>.
 */
public class GoogleSearchSeleniumService {

	private static final Logger LOG = LoggerFactory.getLogger(GoogleSearchSeleniumService.class);
	private static final Pattern PATTERN_NUMBER = Pattern.compile(".*\\s([\\d|.]*)\\s.*");
	private ChromeDriver driver;

	/** Starts a {@link ChromeDriver chrome driver} instance */
	public GoogleSearchSeleniumService() {
		System.setProperty("webdriver.chrome.driver", "C:\\projects\\IoTPlatformIntegrator\\chromedriver.exe");
		final ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless");
		driver = new ChromeDriver(options);
	}

	/** Uses the Google WebSearch with the given {@link String term}. */
	public long search(String term) {
		try {
			// encode the term
			String encodedTerm = URLEncoder.encode(term, "UTF-8");

			// call the URL
			String baseUrl = "https://www.google.de/search?q=" + encodedTerm;
			driver.get(baseUrl);

			// parse the resulting HTML
			Document doc = Jsoup.parse(driver.getPageSource());

			// find the search result count
			Element elem = doc.getElementById("resultStats");

			if (elem == null) {
				// no search result count -> probably ran into capture -> wait 30s and restart
				// chrome driver
				LOG.info("Ran into capture with term {}, waiting for 30 seconds and starting up a new chrome driver instance...", term);
				Thread.sleep(30000);
				final ChromeOptions options = new ChromeOptions();
				options.addArguments("--headless");
				driver = new ChromeDriver(options);
				driver.get(baseUrl);
				doc = Jsoup.parse(driver.getPageSource());

				// search result count should be present by now
				elem = doc.getElementById("resultStats");
			}

			// element "topstuff" does only have child nodes if the search was redirected
			// (e.g. because of a typo) and no results were found for the original search
			// request
			Element topstuffDiv = doc.getElementById("topstuff");
			if (topstuffDiv.childNodeSize() != 0) {
				return 0;
			} else {
				Matcher m = PATTERN_NUMBER.matcher(elem.ownText());
				if (m.find()) {
					BigInteger val = new BigInteger(m.group(1).replace(".", ""));
					LOG.info("Retrieved Google browser search result for '{}'.", term);
					return val.longValue();
				} else {
					LOG.info("Ran into another capture, waiting for 30 seconds...");
					Thread.sleep(30000);
				}
			}

		} catch (UnsupportedEncodingException | WebDriverException | NullPointerException | InterruptedException e) {
			LOG.error(e.getMessage() + " element {} ", term, e);
		} finally {
		}
		return -1;
	}

	public void dispose() {
		driver.close();
	}
}
