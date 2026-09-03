package org.practice;

import java.net.URI;

import org.practice.crawler.WebCrawler;
import org.practice.crawler.model.CrawlerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) throws InterruptedException {
        CrawlerConfiguration configuration = validateArgs(args);
        log.info("Starting crawler with configuration: SeedURI: [{}], timeOutSeconds: [{}], numberOfWorkers: [{}]",
                configuration.seedUri(), configuration.timeoutSeconds(), configuration.workers());

        new WebCrawler(configuration).crawl();
    }

    static CrawlerConfiguration validateArgs(String[] args) {
        if (args == null || args.length != 3) {
            throw new IllegalArgumentException("Usage: <seed URL> <timeout in seconds> <number of workers>");
        }

        URI seedUri = URI.create(args[0]);
        int timeoutSeconds;
        int workers;
        try {
            timeoutSeconds = Integer.parseInt(args[1]);
            workers = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Timeout and worker count must be positive integers", e);
        }
        return new CrawlerConfiguration(seedUri, timeoutSeconds, workers);
    }
}
