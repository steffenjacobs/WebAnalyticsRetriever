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

public class GoogleSearchSeleniumService {

	private static final Logger LOG = LoggerFactory.getLogger(GoogleSearchSeleniumService.class);
	private static final Pattern PATTERN_NUMBER = Pattern.compile(".*\\s([\\d|.]*)\\s.*");
	private ChromeDriver driver;

	public GoogleSearchSeleniumService() {
		System.setProperty("webdriver.chrome.driver", "C:\\projects\\IoTPlatformIntegrator\\chromedriver.exe");
		final ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless");
		driver = new ChromeDriver(options);
	}

	public long search(String term) {
		try {
			String encodedTerm = URLEncoder.encode(term, "UTF-8");

			String baseUrl = "https://www.google.de/search?q=" + encodedTerm;
			driver.get(baseUrl);

			Document doc = Jsoup.parse(driver.getPageSource());

			Element elem = doc.getElementById("resultStats");

			if (elem == null) {
				LOG.info("Ran into capture with term {}, waiting for 30 seconds and starting up a new chrome driver instance...", term);
				Thread.sleep(30000);
				final ChromeOptions options = new ChromeOptions();
				options.addArguments("--headless");
				driver = new ChromeDriver(options);
				driver.get(baseUrl);
				doc = Jsoup.parse(driver.getPageSource());

				elem = doc.getElementById("resultStats");
			}

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
