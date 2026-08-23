package org.practice.crawler;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class CrawlScopeUnitTest {

    private static final CrawlScope MONZO_SCOPE = new CrawlScope(URI.create("https://crawlme.monzo.com/"));

    @Test
    void inScope_acceptsSameHost() {
        assertTrue(MONZO_SCOPE.inScope(URI.create("https://crawlme.monzo.com/about")));
    }

    @Test
    void inScope_isCaseInsensitiveForHost() {
        assertTrue(MONZO_SCOPE.inScope(URI.create("https://CRAWLME.MONZO.COM/about")));
    }

    @Test
    void inScope_rejectsOtherSubdomain() {
        assertFalse(MONZO_SCOPE.inScope(URI.create("https://community.monzo.com/")));
    }

    @Test
    void inScope_rejectsParentDomain() {
        assertFalse(MONZO_SCOPE.inScope(URI.create("https://monzo.com/")));
    }

    @Test
    void inScope_rejectsExternalDomain() {
        assertFalse(MONZO_SCOPE.inScope(URI.create("https://facebook.com/")));
    }

    @Test
    void inScope_rejectsNonHttpScheme() {
        assertFalse(MONZO_SCOPE.inScope(URI.create("mailto:hello@crawlme.monzo.com")));
    }

    @Test
    void inScope_acceptsHttpAndHttpsSchemes() {
        assertTrue(MONZO_SCOPE.inScope(URI.create("http://crawlme.monzo.com/")));
        assertTrue(MONZO_SCOPE.inScope(URI.create("https://crawlme.monzo.com/")));
    }

    @Test
    void inScope_rejectsNonHttpSchemeEvenWithMatchingHost() {
        assertFalse(MONZO_SCOPE.inScope(URI.create("ftp://crawlme.monzo.com/file")));
    }

    @Test
    void inScope_rejectsNullUri() {
        assertFalse(MONZO_SCOPE.inScope(null));
    }
}
