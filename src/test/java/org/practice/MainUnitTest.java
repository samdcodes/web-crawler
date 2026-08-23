package org.practice;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.practice.crawler.model.CrawlerConfiguration;

public class MainUnitTest {

    @Test
    void validateArgs_acceptsAbsoluteHttpSeedUri() {
        CrawlerConfiguration configuration = Main.validateArgs(
                new String[]{"http://crawlme.monzo.com/", "10", "2"});

        assertEquals(URI.create("http://crawlme.monzo.com/"), configuration.seedUri());
        assertEquals(10, configuration.timeoutSeconds());
        assertEquals(2, configuration.workers());
    }

    @Test
    void validateArgs_acceptsAbsoluteHttpsSeedUri() {
        CrawlerConfiguration configuration = Main.validateArgs(
                new String[]{"https://localhost:8080/", "60", "2"});

        assertEquals(URI.create("https://localhost:8080/"), configuration.seedUri());
        assertEquals(60, configuration.timeoutSeconds());
    }

    @Test
    void validateArgs_acceptsCaseInsensitiveHttpScheme() {
        CrawlerConfiguration configuration = Main.validateArgs(
                new String[]{"HTTP://crawlme.monzo.com/", "10", "2"});

        assertEquals(URI.create("HTTP://crawlme.monzo.com/"), configuration.seedUri());
    }

    @Test
    void validateArgs_rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Main.validateArgs(null));
    }

    @Test
    void validateArgs_rejectsTooFewArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "10"}));
    }

    @Test
    void validateArgs_rejectsTooManyArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "10", "2", "extra"}));
    }

    @Test
    void validateArgs_rejectsRelativeSeedUri() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"/start", "10", "2"}));
    }

    @Test
    void validateArgs_rejectsUnsupportedSeedUriScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"ftp://example.com/file", "10", "2"}));
    }

    @Test
    void validateArgs_rejectsSeedUriWithoutHost() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https:/path", "10", "2"}));
    }

    @Test
    void validateArgs_rejectsMalformedSeedUri() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"http://[invalid", "10", "2"}));
    }

    @Test
    void validateArgs_rejectsNonNumericTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "PT1M", "2"}));
    }

    @Test
    void validateArgs_rejectsNonNumericWorkerCount() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "10", "two"}));
    }

    @Test
    void validateArgs_rejectsZeroTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "0", "2"}));
    }

    @Test
    void validateArgs_rejectsNegativeTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "-1", "2"}));
    }

    @Test
    void validateArgs_rejectsZeroWorkers() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "10", "0"}));
    }

    @Test
    void validateArgs_rejectsNegativeWorkers() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.validateArgs(new String[]{"https://crawlme.monzo.com/", "10", "-1"}));
    }
}
