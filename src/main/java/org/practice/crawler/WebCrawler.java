package org.practice.crawler;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.practice.crawler.model.CrawledPage;
import org.practice.crawler.model.CrawlerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebCrawler {

    private static final Logger log = LoggerFactory.getLogger(WebCrawler.class);

    private final URI seedUri;
    private final int workers;
    private final long timeout;
    private final PageFetcher fetcher;
    private final CrawlScope scope;

    private final BlockingQueue<URI> workQueue = new LinkedBlockingQueue<>();

    // Set on items which have been queued to avoid same link being placed on queue multiple times
    private final Set<URI> queued = ConcurrentHashMap.newKeySet();

    // Successfully crawled pages
    private final Collection<CrawledPage> results = new ConcurrentLinkedQueue<>();


    private final AtomicInteger pending = new AtomicInteger();
    private final CountDownLatch done = new CountDownLatch(1);


    public WebCrawler(CrawlerConfiguration configuration) {
        this(configuration, new PageFetcher(new CrawlScope(configuration.seedUri())));
    }

    WebCrawler(CrawlerConfiguration configuration, PageFetcher fetcher) {
        this.seedUri = configuration.seedUri();
        this.workers = configuration.workers();
        this.timeout = configuration.timeoutSeconds();
        this.fetcher = fetcher;
        this.scope = new CrawlScope(configuration.seedUri());
    }

    public List<CrawledPage> crawl() throws InterruptedException {
        submit(UriNormaliser.normalise(seedUri));
        ExecutorService pool = Executors.newFixedThreadPool(workers, workerThreadFactory());
        try {
            for (int i = 0; i < workers; i++) {
                pool.submit(new WebCrawlerWorker(workQueue, fetcher, this));
            }
            // waits for the countDownLatch to be decremented to 0, which is triggered when pending is 0 (meaning no more work is queued or being processed)
            if (!done.await(timeout, TimeUnit.SECONDS)) {
                log.warn("Crawl deadline of {}s reached before completion", timeout);
            }
        } finally {
            pool.shutdownNow();
        }
        log.info("Finished crawling {}. Fetched {} page(s).", seedUri, results.size());
        return List.copyOf(results);
    }

    // Thread factory to set threads as Daemons to properly respect the given timeout
    private static ThreadFactory workerThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "crawler-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    void submit(URI uri) {
        // if not in scope or already in queued, do nothing
        if (!scope.inScope(uri) || !queued.add(uri)) {
            return;
        }
        pending.incrementAndGet();
        workQueue.add(uri);
    }

    void record(CrawledPage page) {
        results.add(page);
        // This add specifically covers redirects
        queued.add(page.uri());
        log.info("\n {}", page);
    }

    void workerFinished() {
        if (pending.decrementAndGet() == 0) {
            done.countDown();
        }
    }
}
