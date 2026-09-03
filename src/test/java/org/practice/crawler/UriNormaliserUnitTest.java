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

    @Test
    void normalise_preservesEncodedCharacters() {
        URI encodedSlash = URI.create("https://example.com/path/a%2Fb"); // %2F decodes to '/'
        URI encodedQuery = URI.create("https://example.com/path?q=a%26b=c"); // %26 decodes to '&'
        assertEquals(encodedSlash, UriNormaliser.normalise(encodedSlash));
        assertEquals(encodedQuery, UriNormaliser.normalise(encodedQuery));
    }

    @Test
    void normalise_stripsTheDefaultPort() {
        URI httpsWithPort = URI.create("https://example.com:443/");
        URI httpsWithoutPort = URI.create("https://example.com/");

        URI httpWithPort = URI.create("http://example.com:80/");
        URI httpWithoutPort = URI.create("http://example.com/");

        assertEquals(httpsWithoutPort, UriNormaliser.normalise(httpsWithPort));
        assertEquals(httpWithoutPort, UriNormaliser.normalise(httpWithPort));
    }
}
