package org.practice.crawler;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Class wrapping Jsoup to handle manual re-directs and re-tries
class PageFetcher {

    private static final Logger log = LoggerFactory.getLogger(PageFetcher.class);

    private static final String USER_AGENT = "PracticeWebCrawler/1.0";
    private static final int REQUEST_TIMEOUT_MS = 10_000;
    private static final int MAX_REDIRECTS = 20; // matches Jsoup's default
    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_BASE_MS = 500;

    // Probably want to flip this to be an 'opt in' rather than 'opt out' approach
    private static final Set<Integer> NON_RETRYABLE_STATUS_CODES = Set.of(
        400, // Bad Request
        401, // Unauthorized
        403, // Forbidden
        404, // Not Found
        405, // Method Not Allowed
        406, // Not Acceptable
        410, // Gone
        414, // URI Too Long
        451  // Unavailable For Legal Reasons
    );

    private final CrawlScope scope;

    public PageFetcher(CrawlScope scope) {
        this.scope = scope;
    }

    public Optional<Document> fetch(URI uri) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return fetchOnce(uri);
            } catch (HttpStatusException e) {
                if (!isRetryable(e.getStatusCode())) {
                    log.warn("Skipping {}: HTTP {} (not retried)", uri, e.getStatusCode());
                    return Optional.empty();
                }
                log.warn("HTTP error for {}: {} (attempt {}/{})",
                    uri, e.getStatusCode(), attempt, MAX_ATTEMPTS);
            } catch (IOException e) {
                log.warn("Failed to fetch {}: {} (attempt {}/{})", uri, e.getMessage(), attempt, MAX_ATTEMPTS);
            }
            if (attempt < MAX_ATTEMPTS && !backoff(attempt)) {
                break;
            }
        }
        return Optional.empty();
    }

    private static boolean isRetryable(int statusCode) {
        return !NON_RETRYABLE_STATUS_CODES.contains(statusCode);
    }


    private Optional<Document> fetchOnce(URI uri) throws IOException {
        URI current = uri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            Connection.Response response = Jsoup.connect(current.toString())
                .userAgent(USER_AGENT)
                // Need to handle redirects manually to ensure that we don't follow out-of-scope redirects.
                .followRedirects(false)
                .ignoreContentType(true)
                .timeout(REQUEST_TIMEOUT_MS)
                .execute();

            if (!isRedirect(response.statusCode())) {
                if (!isHtml(response.contentType())) {
                    log.info("Skipping non-HTML response from {}: {}", current, response.contentType());
                    return Optional.empty();
                }
                return Optional.of(response.parse());
            }

            Optional<URI> next = redirectTarget(current, response);
            if (next.isEmpty()) {
                return Optional.empty();
            }
            current = next.get();
        }
        log.warn("Too many redirects starting from {}", uri);
        return Optional.empty();
    }

    private Optional<URI> redirectTarget(URI current, Connection.Response response) {
        String location = response.header("Location");
        if (location == null) {
            log.warn("Redirect without a Location header from {}", current);
            return Optional.empty();
        }
        URI target;
        try {
            target = UriNormaliser.normalise(current.resolve(location));
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring invalid redirect from {} to {}", current, location, e);
            return Optional.empty();
        }
        if (!scope.inScope(target)) {
            log.warn("Not following out-of-scope redirect from {} to {}", current, target);
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private static boolean backoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_BASE_MS * attempt);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }

    private static boolean isHtml(String contentType) {
        return contentType != null
            && contentType.toLowerCase(Locale.ROOT).contains("text/html");
    }
}
