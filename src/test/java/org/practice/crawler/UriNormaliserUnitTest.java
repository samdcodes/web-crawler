package org.practice.crawler;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class UriNormaliserUnitTest {

    @Test
    void normalise_removesFragment() {
        assertEquals(URI.create("http://example.com/page"),
                UriNormaliser.normalise(URI.create("http://example.com/page#section")));
    }

    @Test
    void normalise_lowerCasesSchemeAndHost() {
        assertEquals(URI.create("http://example.com/page"),
                UriNormaliser.normalise(URI.create("HTTP://Example.COM/page")));
    }

    @Test
    void normalise_preservesPort() {
        assertEquals(URI.create("http://example.com:8080/page"),
                UriNormaliser.normalise(URI.create("http://example.com:8080/page")));
    }

    @Test
    void normalise_resolvesDotSegments() {
        assertEquals(URI.create("http://example.com/a/b"),
                UriNormaliser.normalise(URI.create("http://example.com/a/./c/../b")));
    }

    @Test
    void normalise_preservesQuery() {
        assertEquals(URI.create("http://example.com/search?q=cats"),
                UriNormaliser.normalise(URI.create("http://example.com/search?q=cats")));
    }

    @Test
    void normalise_returnsOpaqueMailtoUriUnchanged() {
        URI mailto = URI.create("mailto:hello@example.com");
        assertEquals(mailto, UriNormaliser.normalise(mailto));
    }

    @Test
    void normalise_returnsOpaqueTelUriUnchanged() {
        URI tel = URI.create("tel:+441234567890");
        assertEquals(tel, UriNormaliser.normalise(tel));
    }
}
