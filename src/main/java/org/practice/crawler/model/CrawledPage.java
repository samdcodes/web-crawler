package org.practice.crawler.model;

import java.net.URI;
import java.util.List;

public record CrawledPage(URI uri, List<Link> links) {

    public CrawledPage {
        links = List.copyOf(links);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("Crawled ").append(uri);
        output.append("\n Links:");
        for (Link link : links) {
            output.append("\n  ").append(link);
        }
        return output.toString();
    }
}
