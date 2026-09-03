# Web Crawler

## Overview

Java web crawler for the monzo tech task. 

## Task Spec

We'd like you to write a simple web crawler in a programming language you're familiar with. 
Given a starting URL, the crawler should visit each URL it finds on the same domain. 
It should print each URL visited, and a list of links found on that page. 
The crawler should be limited to one subdomain - so when you start with *https://crawlme.monzo.com/*, it would crawl all pages on the crawlme.monzo.com website, but not follow external links, for example to facebook.com, monzo.com or community.monzo.com.

Please do not use frameworks like scrapy or go-colly which handle all the crawling behind the scenes or someone else's code. You are welcome to use libraries to handle things like HTML parsing.

We are more interested in how you design software. This means that we care less about a fancy UI or sitemap format, and more about how your program is structured: the trade-offs you've made, what behaviour the program exhibits, and your use of concurrency, test coverage, and so on.

## Prerequisites

- Java 25 
- Apache Maven 3.9.16

## Running

Compile the project:

```bash
mvn clean compile
```

Run the crawler with a seed URL, timeout in seconds, and worker count:

```bash
mvn exec:java -Dexec.args="https://crawlme.monzo.com/ 60 4"
```

The seed URL must be an absolute HTTP(S) URL with a host. The timeout and worker count must be positive integers.

## Testing

```bash
mvn clean test
```

## Design
- A `WebCrawler` which manages state and concurrency 
    - Uses a fixed set of worker threads with a single work queue. 
    - Each worker polls the work queue for a URL, fetches it via an Http request helper, prints the page and its links, and feeds in-scope discovered links back onto the queue.
    - A `queued` set acts as a SoT for pages already queued to be fetched.
    - A `pending` counter is used to track completion, 
      - It is incremented before a URL is enqueued 
      - It is decremented when a worker finishes it
      - When it reaches zero it trips the `CountDownLatch` that ends the crawl.
- Some url normalisation is done to reduce duplication, such as:
    - lower-cases of the scheme and host
    - removes URL fragments
    - resolves dot-segments
    - drops the default port (e.g. http://monzo.com:80 is treated the same as http://monzo.com)
- `PageFetcher` uses jsoup for the request and HTML parsing. Redirects are followed manually so each hop is scope-checked before it is fetched. 
- Permanent client errors are not retried; other HTTP error responses are retried with a linear backoff.
- Non-HTML responses are skipped.
- No interfaces to avoid pre-emptive abstraction


## Limitations

- Does not respect robots.txt
- No global rate limiting or respecting of retry times such as `Retry-After`
- Retry mechanism is fairly immature
- Deduplicates on the normalised URL only, not on page content.
- Scope is host-only, so different ports or schemes on the same host are in scope but treated as distinct URLs.
- No persistent state, output is written to stdout only.
- There is no maximum pages or depth bound limit
 

