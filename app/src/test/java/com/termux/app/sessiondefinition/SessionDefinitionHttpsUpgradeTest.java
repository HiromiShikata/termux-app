package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class SessionDefinitionHttpsUpgradeTest {

    @Test
    public void rewriteKeepsPathAndQuery() {
        assertEquals("https://203.0.113.10:8443/console/sess?k=abc&x=1",
            SessionDefinitionHttpsUpgrade.rewrite(
                "http://203.0.113.10:8080/console/sess?k=abc&x=1",
                "https://203.0.113.10:8443"));
    }

    @Test
    public void rewriteRemovesTrailingSlashWhenOriginalHasNoPathOrQuery() {
        assertEquals("https://h.example:2",
            SessionDefinitionHttpsUpgrade.rewrite("http://h.example:1", "https://h.example:2/"));
    }

    @Test
    public void isPlainHttpRecognizesOnlyTheHttpScheme() {
        assertTrue(SessionDefinitionHttpsUpgrade.isPlainHttp("http://a"));
        assertTrue(SessionDefinitionHttpsUpgrade.isPlainHttp("HTTP://a"));
        assertFalse(SessionDefinitionHttpsUpgrade.isPlainHttp("https://a"));
        assertFalse(SessionDefinitionHttpsUpgrade.isPlainHttp(""));
        assertFalse(SessionDefinitionHttpsUpgrade.isPlainHttp(null));
        assertFalse(SessionDefinitionHttpsUpgrade.isPlainHttp("ftp://a"));
    }

    @Test
    public void endpointForOmitsTheOriginalQueryAndToken() {
        String endpoint = SessionDefinitionHttpsUpgrade.endpointFor("http://203.0.113.10:8080/console?k=SECRET");
        assertEquals("http://203.0.113.10:8080/api/console/https-endpoint", endpoint);
        assertFalse(endpoint.contains("k="));
        assertFalse(endpoint.contains("SECRET"));
    }

    @Test
    public void resolveLeavesHttpsUrlsUntouchedWithoutCallingFetcher() {
        AtomicInteger calls = new AtomicInteger();
        String url = "https://a/definition?k=SECRET";
        assertEquals(url, SessionDefinitionHttpsUpgrade.resolve(url, endpoint -> {
            calls.incrementAndGet();
            return "{}";
        }));
        assertEquals(0, calls.get());
    }

    @Test
    public void resolveLeavesNullUrlsUntouchedWithoutCallingFetcher() {
        AtomicInteger calls = new AtomicInteger();
        assertNull(SessionDefinitionHttpsUpgrade.resolve(null, endpoint -> {
            calls.incrementAndGet();
            return "{}";
        }));
        assertEquals(0, calls.get());
    }

    @Test
    public void productionFetcherResolvesValidHttpsBaseUrl() throws IOException {
        HttpServer server = startServer(200, "{\"httpsBaseUrl\":\"https://127.0.0.1:8443\"}", null, null);
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/s?k=SECRET";
            assertEquals("https://127.0.0.1:8443/s?k=SECRET",
                SessionDefinitionHttpsUpgrade.resolve(url, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void productionFetcherSendsNoTokenOrQueryParameter() throws IOException {
        AtomicReference<String> requestUri = new AtomicReference<>();
        AtomicReference<String> tokenHeader = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        HttpServer server = startServer(200, "{\"httpsBaseUrl\":\"https://127.0.0.1:8443\"}",
            requestUri, new AtomicReference[] {tokenHeader, authorizationHeader});
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/s?k=SECRET";
            SessionDefinitionHttpsUpgrade.resolve(url, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER);
            assertNull(tokenHeader.get());
            assertNull(authorizationHeader.get());
            assertFalse(requestUri.get().contains("k="));
            assertFalse(requestUri.get().contains("SECRET"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void productionFetcherLeavesUrlUnchangedForNotFound() throws IOException {
        assertProductionResultUnchanged(404, "", null);
    }

    @Test
    public void productionFetcherLeavesUrlUnchangedForMalformedJson() throws IOException {
        assertProductionResultUnchanged(200, "{not json", null);
    }

    @Test
    public void productionFetcherLeavesUrlUnchangedForNonHttpsBaseUrl() throws IOException {
        assertProductionResultUnchanged(200, "{\"httpsBaseUrl\":\"http://127.0.0.1:8443\"}", null);
    }

    @Test
    public void productionFetcherLeavesUrlUnchangedForDifferentBaseHost() throws IOException {
        assertProductionResultUnchanged(200, "{\"httpsBaseUrl\":\"https://evil.example:8443\"}", null);
    }

    @Test
    public void productionFetcherDoesNotFollowRedirects() throws IOException {
        assertProductionResultUnchanged(308, "", "https://127.0.0.1:8443/redirected");
    }

    @Test
    public void resolveLeavesUrlUnchangedWhenFetcherThrowsIOException() {
        String url = "http://a/definition?k=SECRET";
        assertEquals(url, SessionDefinitionHttpsUpgrade.resolve(url, endpoint -> {
            throw new IOException("failure");
        }));
    }

    @Test
    public void resolveLeavesUrlUnchangedWhenFetcherThrowsRuntimeException() {
        String url = "http://a/definition?k=SECRET";
        assertEquals(url, SessionDefinitionHttpsUpgrade.resolve(url, endpoint -> {
            throw new IllegalStateException("failure");
        }));
    }

    @Test
    public void parseHttpsBaseUrlReturnsOnlyValidJsonStringValues() {
        assertNull(SessionDefinitionHttpsUpgrade.parseHttpsBaseUrl(""));
        assertNull(SessionDefinitionHttpsUpgrade.parseHttpsBaseUrl("{}"));
        assertEquals("https://a:1",
            SessionDefinitionHttpsUpgrade.parseHttpsBaseUrl("{\"httpsBaseUrl\":\"https://a:1\"}"));
    }

    @Test
    public void validatedHttpsBaseUrlRejectsUnsafeValuesAndAcceptsDifferentPort() {
        assertNull(SessionDefinitionHttpsUpgrade.validatedHttpsBaseUrl("http://a:8080", "https://a:1/x"));
        assertNull(SessionDefinitionHttpsUpgrade.validatedHttpsBaseUrl("http://a:8080", "https://a:1?q=1"));
        assertNull(SessionDefinitionHttpsUpgrade.validatedHttpsBaseUrl("http://a:8080", "https://u@a:1"));
        assertNull(SessionDefinitionHttpsUpgrade.validatedHttpsBaseUrl("http://a:8080", "https://b:1"));
        assertEquals("https://a:8443",
            SessionDefinitionHttpsUpgrade.validatedHttpsBaseUrl("http://a:8080", "https://a:8443"));
    }

    private static void assertProductionResultUnchanged(int status, String responseBody, String location)
        throws IOException {
        HttpServer server = startServer(status, responseBody, null, null, location);
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/s?k=SECRET";
            assertEquals(url,
                SessionDefinitionHttpsUpgrade.resolve(url, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(int status, String responseBody, AtomicReference<String> requestUri,
        AtomicReference<String>[] headers) throws IOException {
        return startServer(status, responseBody, requestUri, headers, null);
    }

    private static HttpServer startServer(int status, String responseBody, AtomicReference<String> requestUri,
        AtomicReference<String>[] headers, String location) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/console/https-endpoint", exchange -> {
            if (requestUri != null) {
                requestUri.set(exchange.getRequestURI().toString());
            }
            if (headers != null) {
                headers[0].set(exchange.getRequestHeaders().getFirst("x-pv-token"));
                headers[1].set(exchange.getRequestHeaders().getFirst("Authorization"));
            }
            if (location != null) {
                exchange.getResponseHeaders().set("Location", location);
            }
            byte[] body = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        return server;
    }
}
