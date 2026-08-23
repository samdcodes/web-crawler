package org.practice.crawler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.practice.crawler.model.CrawledPage;
import org.practice.crawler.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WebCrawlerWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(WebCrawlerWorker.class);

    private final BlockingQueue<URI> workQueue;
    private final PageFetcher fetcher;
    private final WebCrawler crawler;

    WebCrawlerWorker(BlockingQueue<URI> workQueue, PageFetcher fetcher, WebCrawler crawler) {
        this.workQueue = workQueue;
        this.fetcher = fetcher;
        this.crawler = crawler;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // take from queue (blocks)
                URI uri = workQueue.take();
                try {
                    // fetch page
                    fetcher.fetch(uri).ifPresent(document -> {

                        // Parse links
                        CrawledPage page = toPage(document);

                        // Log/store page
                        crawler.record(page);

                        // Submit each link to be crawled (if valid)
                        for (Link link : page.links()) {
                            toUri(link.url()).ifPresent(crawler::submit);
                        }
                    });
                } catch (RuntimeException e) {
                    log.error("Unexpected error crawling {}: {}", uri, e.getMessage(), e);
                } finally {
                    // decrement worker
                    crawler.workerFinished();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static CrawledPage toPage(Document document) {
        URI uri = UriNormaliser.normalise(URI.create(document.location()));
        List<Link> links = new ArrayList<>();
        for (Element element : document.select("a[href]")) {
            links.add(new Link(element.text(), element.absUrl("href")));
        }
        return new CrawledPage(uri, links);
    }

    private static Optional<URI> toUri(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UriNormaliser.normalise(URI.create(value)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
