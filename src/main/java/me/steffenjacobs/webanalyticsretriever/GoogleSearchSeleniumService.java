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

	public long search(String term) {
		try {
			String encodedTerm = URLEncoder.encode(term, "UTF-8");
			System.setProperty("webdriver.chrome.driver", "C:\\projects\\IoTPlatformIntegrator\\chromedriver.exe");
			final ChromeOptions options = new ChromeOptions();
			options.addArguments("--headless");
			ChromeDriver driver = new ChromeDriver(options);

			String baseUrl = "https://www.google.de/search?q=" + encodedTerm;
			driver.get(baseUrl);

			Document doc = Jsoup.parse(driver.getPageSource());

			Element elem = doc.getElementById("resultStats");

			Matcher m = PATTERN_NUMBER.matcher(elem.ownText());

			if (m.find()) {
				BigInteger val = new BigInteger(m.group(1).replace(".", ""));
				driver.close();
				LOG.info("Retrieved Google browser search result for '{}'.", term);
				return val.longValue();
			}
			driver.close();

		} catch (UnsupportedEncodingException  | WebDriverException e) {
			LOG.error(e.getMessage(), e);
		}
		finally {
		}
		return -1;
	}
}
