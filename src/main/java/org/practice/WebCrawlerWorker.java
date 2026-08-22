package org.practice;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawlerWorker implements Runnable {
    private static final int MAX_RETRIES = 3;
    private static final Logger log = LoggerFactory.getLogger(WebCrawlerWorker.class.getName());

    private final BlockingQueue<CrawlTask> workQueue;
    private final Set<URI> visited;
    private final URI initialHost;
    private final AtomicInteger workCount;

    public WebCrawlerWorker(BlockingQueue<CrawlTask> workQueue, AtomicInteger workCount, Set<URI> visited, URI initialHost) {
        this.workQueue = workQueue;
        this.visited = visited;
        this.initialHost = initialHost;
        this.workCount = workCount;
    }

    @Override
    public void run() {
        while (workCount.get() > 0) {
            CrawlTask task;
            try {
                task = workQueue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            if (task == null) {
                continue;
            }
            URI uri = task.uri();

            try {
                log.info("Crawling {}", uri);
                Document document = Jsoup.connect(uri.toString()).timeout(10_000).get();
                processDocument(document);
            } catch (IOException e) {
                log.error(e.getMessage());
                log.debug(e.getMessage(), e);
                if (task.attempts() < MAX_RETRIES) {
                    workQueue.add(new CrawlTask(uri, task.attempts() + 1));
                    workCount.incrementAndGet();
                }
            } finally {
                workCount.decrementAndGet();
            }

        }
    }

    private void processDocument(Document document) {
        Elements links = document.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            String text = link.text();
            String absUrl = link.absUrl("href");
            log.info("Found Links: {} - {} - {}", href, text, absUrl);

            if (absUrl.isEmpty()) {
                continue;
            }

            URI newUri;
            try {
                newUri = normalize(URI.create(absUrl));
            } catch (IllegalArgumentException e) {
                log.debug("Ignoring invalid link: {}", absUrl, e);
                continue;
            }

            if (initialHost.getHost().equalsIgnoreCase(newUri.getHost()) && visited.add(newUri)) {
                workQueue.add(new CrawlTask(newUri, 0));
                workCount.incrementAndGet();
            }
        }
    }

    private static URI normalize(URI uri) {
        return URI.create(uri.toString().split("#", 2)[0]);
    }
}
