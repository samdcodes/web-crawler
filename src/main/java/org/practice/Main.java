package org.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class.getName());

    static void main(String[] args) throws InterruptedException {
        try {
            CrawlerConfiguration configuration = validateArgs(args);
            log.info("Starting crawler with configuration: SeedURI: [{}], timeOutMinutes: [{}], numberOfWorkers: [{}]",
                    configuration.seedUri(), configuration.timeoutMinutes(), configuration.workers());
            new WebCrawler(configuration.seedUri(), configuration.workers(), configuration.timeoutMinutes()).crawl();
        } catch (IllegalArgumentException e) {
            log.error("Invalid crawler configuration: {}", e.getMessage());
        }
    }

    private static CrawlerConfiguration validateArgs(String[] args) {
        if (args == null || args.length != 3) {
            throw new IllegalArgumentException("Usage: <seed URL> <timeout in minutes> <number of workers>");
        }

        try {
            URI seedUri = URI.create(args[0]);
            int timeoutMinutes = Integer.parseInt(args[1]);
            int workers = Integer.parseInt(args[2]);
            
            if (timeoutMinutes < 1 || workers < 1) {
                throw new IllegalArgumentException("Timeout and worker count must be positive");
            }
            
            return new CrawlerConfiguration(seedUri, timeoutMinutes, workers);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Timeout and worker count must be integers", e);
        }
    }
}
