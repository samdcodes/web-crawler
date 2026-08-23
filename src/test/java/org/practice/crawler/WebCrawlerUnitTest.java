package org.practice.crawler;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.practice.crawler.model.CrawledPage;
import org.practice.crawler.model.CrawlerConfiguration;

public class WebCrawlerUnitTest {

    private static final URI SEED = URI.create("http://example.com/");

    private static CrawlerConfiguration config(int timeoutSeconds, int workers) {
        return new CrawlerConfiguration(SEED, timeoutSeconds, workers);
    }


    private static Document document(String url, String... linkUrls) {
        URI uri = UriNormaliser.normalise(URI.create(url));
        StringBuilder html = new StringBuilder("<html><body>");
        for (String link : linkUrls) {
            html.append("<a href=\"").append(link).append("\">").append(link).append("</a>");
        }
        html.append("</body></html>");
        return Jsoup.parse(html.toString(), uri.toString());
    }

    @Test
    void crawl_processesSeedUri() throws InterruptedException {
        PageFetcher fetcher = mock(PageFetcher.class);
        when(fetcher.fetch(SEED)).thenReturn(Optional.of(document("http://example.com/")));

        List<CrawledPage> pages = new WebCrawler(config(5, 2), fetcher).crawl();

        assertEquals(1, pages.size());
        assertEquals(SEED, pages.get(0).uri());
    }

    @Test
    void crawl_recordsFetchedUrlNotBaseHrefOverride() throws InterruptedException {
        PageFetcher fetcher = mock(PageFetcher.class);
        // Fetched from SEED, but the page declares a <base> pointing to a different host.
        // The recorded identity must be the fetched URL (location()), not the <base> href.
        Document withBase = Jsoup.parse(
                "<head><base href=\"http://other.com/sub/\"></head><body></body>", SEED.toString());
        when(fetcher.fetch(SEED)).thenReturn(Optional.of(withBase));

        List<CrawledPage> pages = new WebCrawler(config(5, 2), fetcher).crawl();

        assertEquals(1, pages.size());
        assertEquals(SEED, pages.get(0).uri());
    }

    @Test
    void crawl_followsInScopeLinksAndIgnoresExternalLinks() throws InterruptedException {
        PageFetcher fetcher = mock(PageFetcher.class);
        URI internal = URI.create("http://example.com/a");
        URI external = URI.create("https://other.com/x");
        when(fetcher.fetch(SEED))
                .thenReturn(Optional.of(document("http://example.com/", internal.toString(), external.toString())));
        when(fetcher.fetch(internal)).thenReturn(Optional.of(document("http://example.com/a")));

        List<CrawledPage> pages = new WebCrawler(config(5, 2), fetcher).crawl();

        List<URI> crawled = pages.stream().map(CrawledPage::uri).toList();
        assertEquals(2, crawled.size());
        assertTrue(crawled.contains(SEED));
        assertTrue(crawled.contains(internal));
        verify(fetcher, never()).fetch(external);
    }

    @Test
    void crawl_fetchesEachPageOnceDespiteDuplicateAndCyclicLinks() throws InterruptedException {
        PageFetcher fetcher = mock(PageFetcher.class);
        URI a = URI.create("http://example.com/a");
        when(fetcher.fetch(SEED))
                .thenReturn(Optional.of(document("http://example.com/", a.toString(), a.toString())));
        when(fetcher.fetch(a)).thenReturn(Optional.of(document("http://example.com/a", "http://example.com/")));

        new WebCrawler(config(5, 3), fetcher).crawl();

        verify(fetcher, times(1)).fetch(SEED);
        verify(fetcher, times(1)).fetch(a);
    }

    @Test
    void crawl_treatsFragmentVariantsAsOnePage() throws InterruptedException {
        PageFetcher fetcher = mock(PageFetcher.class);
        URI a = URI.create("http://example.com/a");
        when(fetcher.fetch(SEED)).thenReturn(Optional.of(
                document("http://example.com/", "http://example.com/a#one", "http://example.com/a#two")));
        when(fetcher.fetch(a)).thenReturn(Optional.of(document("http://example.com/a")));

        new WebCrawler(config(5, 2), fetcher).crawl();

        verify(fetcher, times(1)).fetch(a);
    }

    @Test
    void crawl_terminatesWhenWorkIsComplete() {
        PageFetcher fetcher = mock(PageFetcher.class);
        when(fetcher.fetch(SEED)).thenReturn(Optional.of(document("http://example.com/", "http://example.com/a")));
        when(fetcher.fetch(URI.create("http://example.com/a")))
                .thenReturn(Optional.of(document("http://example.com/a", "http://example.com/b")));
        when(fetcher.fetch(URI.create("http://example.com/b")))
                .thenReturn(Optional.of(document("http://example.com/b")));

        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> new WebCrawler(config(30, 2), fetcher).crawl());
    }

    @Test
    void crawl_completesWhenSeedFetchReturnsEmpty() {
        PageFetcher fetcher = mock(PageFetcher.class);
        when(fetcher.fetch(SEED)).thenReturn(Optional.empty());

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            List<CrawledPage> pages = new WebCrawler(config(30, 2), fetcher).crawl();
            assertTrue(pages.isEmpty());
        });
    }

    @Test
    void crawl_continuesWhenAPageFetchThrows() {
        PageFetcher fetcher = mock(PageFetcher.class);
        URI bad = URI.create("http://example.com/bad");
        URI good = URI.create("http://example.com/good");
        when(fetcher.fetch(SEED))
                .thenReturn(Optional.of(document("http://example.com/", bad.toString(), good.toString())));
        when(fetcher.fetch(bad)).thenThrow(new RuntimeException("Error"));
        when(fetcher.fetch(good)).thenReturn(Optional.of(document("http://example.com/good")));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            List<CrawledPage> pages = new WebCrawler(config(30, 2), fetcher).crawl();
            List<URI> crawled = pages.stream().map(CrawledPage::uri).toList();
            assertEquals(2, crawled.size());
            assertTrue(crawled.contains(SEED));
            assertTrue(crawled.contains(good));
        });
    }

    @Test
    void crawl_stopsAfterTimeout() {
        CountDownLatch started = new CountDownLatch(1);
        PageFetcher fetcher = mock(PageFetcher.class);
        when(fetcher.fetch(any())).thenAnswer(invocation -> {
            started.countDown();
            try {
                Thread.sleep(Duration.ofMinutes(1).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        });

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            List<CrawledPage> pages = new WebCrawler(config(1, 2), fetcher).crawl();
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(pages.isEmpty());
        });
    }
}
