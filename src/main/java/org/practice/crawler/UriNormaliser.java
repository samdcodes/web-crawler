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
        String normalisedHost = lower(normalised.getHost());
        try {
            return new URI(scheme, normalised.getUserInfo(), normalisedHost, normalised.getPort(), normalised.getPath(), normalised.getQuery(), null);
        } catch (URISyntaxException e) {
            log.warn("Failed to normalise {}: {}", uri, e.getMessage());
            return uri;
        }
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
