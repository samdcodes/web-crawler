package org.practice.crawler;

import java.net.URI;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

public class PageFetcherUnitTest {

    private WireMockServer server;
    private URI seedUri;
    private PageFetcher fetcher;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        seedUri = URI.create("http://127.0.0.1:" + server.port() + "/");
        fetcher = new PageFetcher(new CrawlScope(seedUri));
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private URI path(String path) {
        return URI.create("http://127.0.0.1:" + server.port() + path);
    }

    @Test
    void fetch_followsInternalRedirectAndReportsResolvedUri() {
        server.stubFor(get("/internal-redirect")
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/internal")));
        server.stubFor(get("/internal")
                .willReturn(aResponse().withHeader("Content-Type", "text/html").withBody("<html></html>")));

        Optional<Document> page = fetcher.fetch(path("/internal-redirect"));

        assertTrue(page.isPresent());
        assertEquals(path("/internal").toString(), page.get().baseUri());
        server.verify(1, getRequestedFor(urlEqualTo("/internal")));
    }

    @Test
    void fetch_doesNotFollowExternalRedirect() {
        WireMockServer externalServer = new WireMockServer(wireMockConfig().dynamicPort());
        externalServer.start();
        try {
            externalServer.stubFor(get("/external").willReturn(aResponse().withStatus(200)));
            server.stubFor(get("/external-redirect")
                    .willReturn(aResponse().withStatus(302)
                            .withHeader("Location", externalServer.baseUrl() + "/external")));

            Optional<Document> page = fetcher.fetch(path("/external-redirect"));

            assertTrue(page.isEmpty());
            externalServer.verify(0, getRequestedFor(urlEqualTo("/external")));
        } finally {
            externalServer.stop();
        }
    }

    @Test
    void fetch_doesNotFollowUnsupportedSchemeRedirect() {
        URI redirectUri = URI.create("ftp://127.0.0.1:" + server.port() + "/unsupported");
        server.stubFor(get("/unsupported-redirect")
                .willReturn(aResponse().withStatus(302).withHeader("Location", redirectUri.toString())));

        Optional<Document> page = fetcher.fetch(path("/unsupported-redirect"));

        assertTrue(page.isEmpty());
    }

    @Test
    void fetch_returnsRawHtmlWithoutTransformingLinks() {
        server.stubFor(get("/start")
                .willReturn(aResponse().withHeader("Content-Type", "text/html")
                        .withBody("<a href=\"/page\">One</a><a href=\"https://example.com/\">Two</a>")));

        Optional<Document> page = fetcher.fetch(path("/start"));

        assertTrue(page.isPresent());
        assertEquals(2, page.get().select("a[href]").size());
        assertEquals(path("/page").toString(), page.get().selectFirst("a").absUrl("href"));
    }

    @Test
    void fetch_doesNotRetryNotFoundResponse() {
        server.stubFor(get("/missing").willReturn(aResponse().withStatus(404)));

        Optional<Document> page = fetcher.fetch(path("/missing"));

        assertTrue(page.isEmpty());
        server.verify(1, getRequestedFor(urlEqualTo("/missing")));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 405, 406, 410, 414, 451})
    void fetch_doesNotRetryPermanentClientErrorResponses(int statusCode) {
        server.stubFor(get("/permanent-error").willReturn(aResponse().withStatus(statusCode)));

        Optional<Document> page = fetcher.fetch(path("/permanent-error"));

        assertTrue(page.isEmpty());
        server.verify(1, getRequestedFor(urlEqualTo("/permanent-error")));
    }

    @Test
    void fetch_retriesServerErrorResponseUpToMaxAttempts() {
        server.stubFor(get("/broken").willReturn(aResponse().withStatus(500)));

        Optional<Document> page = fetcher.fetch(path("/broken"));

        assertTrue(page.isEmpty());
        server.verify(3, getRequestedFor(urlEqualTo("/broken")));
    }

    @Test
    void fetch_doesNotParseNonHtmlResponse() {
        server.stubFor(get("/asset.pdf")
                .willReturn(aResponse().withHeader("Content-Type", "application/pdf")
                        .withBody("<a href=\"/page\">Not HTML</a>")));

        Optional<Document> page = fetcher.fetch(path("/asset.pdf"));

        assertTrue(page.isEmpty());
        server.verify(0, getRequestedFor(urlEqualTo("/page")));
    }

    @Test
    void fetch_retriesTransientErrorThenSucceeds() {
        server.stubFor(get("/flaky").inScenario("transient")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("recovered"));
        server.stubFor(get("/flaky").inScenario("transient")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse().withHeader("Content-Type", "text/html").withBody("<html></html>")));

        Optional<Document> page = fetcher.fetch(path("/flaky"));

        assertTrue(page.isPresent());
        server.verify(2, getRequestedFor(urlEqualTo("/flaky")));
    }

    @Test
    void fetch_stopsAfterTooManyRedirects() {
        server.stubFor(get("/loop")
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/loop")));

        Optional<Document> page = fetcher.fetch(path("/loop"));

        assertTrue(page.isEmpty());
    }
}
