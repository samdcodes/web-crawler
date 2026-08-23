package org.practice.crawler.model;

public record Link(
        String text,
        String url // stored as String as we still need to print invalid/out of scope links
) {
    @Override
    public String toString() {
        return text + " - " + url;
    }
}
