package org.practice.crawler.model;

import java.net.URI;

// config for the Crawler with some validation
public record CrawlerConfiguration(URI seedUri, int timeoutSeconds, int workers) {

    public CrawlerConfiguration {
        String scheme = seedUri.getScheme();
        boolean isHttpOrHttps = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!seedUri.isAbsolute() || seedUri.getHost() == null || !isHttpOrHttps) {
            throw new IllegalArgumentException("Seed URL must be an absolute HTTP(S) URL with a host");
        }
        if (timeoutSeconds < 1 || workers < 1) {
            throw new IllegalArgumentException("Timeout and worker count must be positive");
        }
    }
}
