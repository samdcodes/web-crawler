package org.practice;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebCrawler {
    private static final Logger log = LoggerFactory.getLogger(WebCrawler.class.getName());

    private final BlockingQueue<CrawlTask> workQueue = new LinkedBlockingQueue<>();
    private final Set<URI> visited = ConcurrentHashMap.newKeySet();
    private final URI seedUri;
    private final int workers;
    private final int timeOutMinutes;
    private final AtomicInteger workCount = new AtomicInteger();

    public WebCrawler(URI seedUri, int workers, int timeOutMinutes) {
        this.seedUri = seedUri;
        this.workers = workers;
        this.timeOutMinutes = timeOutMinutes;
    }

    public void crawl() throws InterruptedException {
        visited.add(seedUri);
        workCount.incrementAndGet();
        workQueue.add(new CrawlTask(seedUri, 0));
        try (ExecutorService executorService = Executors.newFixedThreadPool(workers)) {
            for (int i = 0; i < workers; i++) {
                log.info("starting worker {}", i + 1);
                executorService.submit(new WebCrawlerWorker(workQueue, workCount, visited, seedUri));
            }
            executorService.shutdown();
            boolean completed = executorService.awaitTermination(timeOutMinutes, TimeUnit.MINUTES);
            if (completed) {
                log.info("Crawler terminated");
            } else {
                log.error("Crawler did not terminate in configured time [{}]", timeOutMinutes);
                executorService.shutdownNow();
            }
        }
        log.info("Finished crawling site: {}", seedUri);
        log.info("Crawled [{}] pages", visited.size());
        log.info("Crawled [{}] pages", visited.stream().toList());
        log.info("Queue size should be 0 - it is: {}", workQueue.size());
    }
}
