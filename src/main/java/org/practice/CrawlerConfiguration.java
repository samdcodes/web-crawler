package org.practice;

import java.net.URI;

record CrawlerConfiguration(URI seedUri, int timeoutMinutes, int workers) {
}
