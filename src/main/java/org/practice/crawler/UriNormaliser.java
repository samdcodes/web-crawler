package org.practice.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//  helper class for normalising URIs
public final class UriNormaliser {

    private static final Logger log = LoggerFactory.getLogger(UriNormaliser.class);

    private UriNormaliser() {
    }

    public static URI normalise(URI uri) {
        // Opaque URIs (e.g mailto:) have no hierarchical host/path to normalise.
        if (uri.isOpaque()) {
            return uri;
        }

        URI normalised = uri.normalize();

        String scheme = lower(normalised.getScheme());
        // DNS is case-insensitive
        String normalisedHost = lower(normalised.getHost());

        // If no scheme or host such as a relative URI, return
        if (scheme == null || normalisedHost == null) {
            return normalised;
        }

        StringBuilder rebuiltUri = new StringBuilder()
                .append(scheme)
                .append("://")
                .append(normalisedHost);

        int port = normalised.getPort();
        if (port != -1 && port != defaultPort(scheme)) {
            rebuiltUri.append(":").append(port);
        }
        // If you don't use the rawPath/rawQuery, elements will be encoded such that /a%2Fb would become /a/b
        // Because %2f is the URL-encoded representation of a forward slash
        String rawPath = normalised.getRawPath();
        rebuiltUri.append(rawPath == null || rawPath.isEmpty() ? "/" : rawPath);

        String rawQuery = normalised.getRawQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            rebuiltUri.append("?").append(rawQuery);
        }

        try {
            return new URI(rebuiltUri.toString());
        } catch (URISyntaxException e) {
            log.warn("Failed to normalise {}: {}", uri, e.getMessage());
            return uri;
        }
    }

    // Some parts of the URI (e.g Protocol / DNS) are case-insensitive
    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    // https://monzo.com is effectively the same as the same https://monzo.com:443
    private static int defaultPort(String scheme) {
        return switch (scheme) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }
}
