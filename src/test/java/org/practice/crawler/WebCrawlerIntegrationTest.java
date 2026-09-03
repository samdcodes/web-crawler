package org.practice.crawler;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.practice.crawler.model.CrawledPage;
import org.practice.crawler.model.CrawlerConfiguration;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class WebCrawlerIntegrationTest {

    private WireMockServer server;
    private URI seedUri;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        seedUri = URI.create("http://127.0.0.1:" + server.port() + "/");
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void crawl_fetchesAndTraversesWebsiteEndToEnd() throws InterruptedException {
        stubHtml("/", """
                <a href="/about">About</a>
                <a href="/about#history">About history</a>
                <a href="/redirect">Contact</a>
                <a href="/asset.pdf">Download</a>
                <a href="http://localhost:%d/external">External</a>
                """.formatted(server.port()));
        stubHtml("/about", "<a href=\"/\">Home</a><a href=\"/team\">Team</a>");
        stubHtml("/team", "<a href=\"/about\">About</a>");
        stubHtml("/contact", "<h1>Contact</h1>");
        server.stubFor(get("/redirect")
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/contact")));
        server.stubFor(get("/asset.pdf")
                .willReturn(aResponse().withHeader("Content-Type", "application/pdf").withBody("PDF")));

        WebCrawler crawler = new WebCrawler(new CrawlerConfiguration(seedUri, 5, 3));

        List<CrawledPage> pages = crawler.crawl();

        Set<URI> crawledUris = pages.stream().map(CrawledPage::uri).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(seedUri, uri("/about"), uri("/team"), uri("/contact")), crawledUris);
        assertEquals(crawledUris.size(), pages.size());
        server.verify(1, getRequestedFor(urlEqualTo("/")));
        server.verify(1, getRequestedFor(urlEqualTo("/about")));
        server.verify(1, getRequestedFor(urlEqualTo("/team")));
        server.verify(1, getRequestedFor(urlEqualTo("/redirect")));
        server.verify(1, getRequestedFor(urlEqualTo("/contact")));
        server.verify(1, getRequestedFor(urlEqualTo("/asset.pdf")));
        server.verify(0, getRequestedFor(urlEqualTo("/external")));
    }

    private void stubHtml(String path, String body) {
        server.stubFor(get(path).willReturn(aResponse()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(body)));
    }

    private URI uri(String path) {
        return seedUri.resolve(path);
    }
}