package org.practice.crawler;

import java.net.URI;

// Holds the seed subdomain and a method for checking if a URI is in scope
public final class CrawlScope {

    private final String seedHost;

    public CrawlScope(URI seedUri) {
        this.seedHost = seedUri.getHost();
    }

    // ensure only http(s) requests are valid and host matches (ignoring case)
    public boolean inScope(URI uri) {
        return uri != null
            && isHttpOrHttps(uri)
            && seedHost.equalsIgnoreCase(uri.getHost());
    }

    private static boolean isHttpOrHttps(URI uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
}
