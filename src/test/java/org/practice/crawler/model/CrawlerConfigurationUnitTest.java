package org.practice.crawler.model;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class CrawlerConfigurationUnitTest {

    private static final URI SEED = URI.create("https://crawlme.monzo.com/");

    @Test
    void constructor_acceptsValidConfiguration() {
        CrawlerConfiguration configuration = new CrawlerConfiguration(SEED, 60, 4);

        assertEquals(SEED, configuration.seedUri());
        assertEquals(60, configuration.timeoutSeconds());
        assertEquals(4, configuration.workers());
    }

    @Test
    void constructor_acceptsHttpSeedUri() {
        URI seed = URI.create("http://crawlme.monzo.com/");

        assertEquals(seed, new CrawlerConfiguration(seed, 1, 1).seedUri());
    }

    @Test
    void constructor_acceptsCaseInsensitiveScheme() {
        URI seed = URI.create("HTTPS://crawlme.monzo.com/");

        assertEquals(seed, new CrawlerConfiguration(seed, 1, 1).seedUri());
    }

    @Test
    void constructor_rejectsRelativeSeedUri() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfiguration(URI.create("/start"), 60, 4));
    }

    @Test
    void constructor_rejectsSeedUriWithoutHost() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfiguration(URI.create("https:/path"), 60, 4));
    }

    @Test
    void constructor_rejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfiguration(URI.create("ftp://example.com/file"), 60, 4));
    }

    @Test
    void constructor_rejectsZeroTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfiguration(SEED, 0, 4));
    }

    @Test
    void constructor_rejectsNegativeTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfiguration(SEED, -1, 4));
    }

    @Test
    void constructor_rejectsZeroWorkers() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfiguration(SEED, 60, 0));
    }

    @Test
    void constructor_rejectsNegativeWorkers() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrawlerConfiguration(SEED, 60, -1));
    }
}
