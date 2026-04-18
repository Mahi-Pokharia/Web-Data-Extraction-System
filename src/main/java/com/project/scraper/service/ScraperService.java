package com.project.scraper.service;

import com.project.scraper.model.WebData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/*Service layer responsible for fetching and parsing web pages with Jsoup*/
@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    /*avoid simple bot-detection blocks*/
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    /*read timeout in milliseconds*/
    private static final int TIMEOUT_MS = 5_000;

    private static final String HEADING_SELECTOR = "h1, h2, h3, h4, h5, h6";

    /**
     * Scrape the given URL and return structured {@link WebData}.
     *
     * @param url the fully-qualified URL to scrape (must start with http/https)
     * @return populated {@link WebData} instance
     * @throws IllegalArgumentException if the URL is null, blank, or malformed
     * @throws IOException              if the connection times out or the server is unreachable
     */
    public WebData scrape(String url) throws IOException {
        validateUrl(url);

        log.info("Scraping URL: {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(false)  
                .get();

        String title = doc.title();

        List<String> headings = new ArrayList<>();
        Elements headingElements = doc.select(HEADING_SELECTOR);
        for (Element el : headingElements) {
            String text = el.text().strip();
            if (!text.isEmpty()) {
                headings.add("[" + el.tagName().toUpperCase() + "] " + text);
            }
        }

        List<String> links = new ArrayList<>();
        Elements anchors = doc.select("a[href]");
        for (Element a : anchors) {
            // absUrl() resolves relative URLs against the document base URI
            String absUrl = a.absUrl("href");
            if (!absUrl.isBlank() && !links.contains(absUrl)) {
                links.add(absUrl);
            }
        }

        log.info("Scraped '{}' → title='{}', headings={}, links={}",
                url, title, headings.size(), links.size());

        return new WebData(url, title, headings, links);
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank.");
        }

        URI uri;
        try {
            uri = new URI(url).parseServerAuthority();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                    "Only http/https URLs are supported. Received scheme: " + scheme);
        }
        
        try {
            var javaUrl = URI.create(url).toURL();
            if (javaUrl.getHost() == null || javaUrl.getHost().isBlank()) {
                throw new IllegalArgumentException("URL has no host: " + url);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
    }
}

