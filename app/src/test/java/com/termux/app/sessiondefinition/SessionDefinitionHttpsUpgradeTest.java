package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
    public void productionFetcherResolvesValidHttpsBaseUrl() {
        String url = "http://127.0.0.1:8080/s?k=SECRET";
        assertEquals("https://127.0.0.1:8443/s?k=SECRET",
            SessionDefinitionHttpsUpgrade.resolve(url, endpoint ->
                endpoint.startsWith("http://127.0.0.1:")
                    || endpoint.equals("https://127.0.0.1:8443/api/console/https-endpoint")
                    ? "{\"httpsBaseUrl\":\"https://127.0.0.1:8443\"}"
                    : null));
    }

    @Test
    public void productionFetcherSendsNoTokenOrQueryParameter() throws IOException {
        LocalHttpServer server = startServer(200, "{\"httpsBaseUrl\":\"https://127.0.0.1:8443\"}", null);
        try {
            String url = "http://127.0.0.1:" + server.port() + "/s?k=SECRET";
            SessionDefinitionHttpsUpgrade.resolve(url, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER);
            assertEquals(1, server.requests().size());
            LocalRequest request = server.requests().get(0);
            assertEquals("GET", request.method);
            assertEquals("/api/console/https-endpoint", request.target);
            assertNull(request.headers.get("x-pv-token"));
            assertNull(request.headers.get("authorization"));
            assertFalse(request.target.contains("k="));
            assertFalse(request.target.contains("SECRET"));
        } finally {
            server.stop();
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
        LocalHttpServer targetServer = startServer(200, "{\"httpsBaseUrl\":\"https://127.0.0.1:8443\"}", null);
        LocalHttpServer server = startServer(308, "", "http://127.0.0.1:" + targetServer.port() + "/redirected");
        try {
            String url = "http://127.0.0.1:" + server.port() + "/s?k=SECRET";
            assertEquals(url,
                SessionDefinitionHttpsUpgrade.resolve(url, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER));
            assertEquals(0, targetServer.requests().size());
        } finally {
            server.stop();
            targetServer.stop();
        }
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
    public void resolveLeavesUrlUnchangedWhenHttpsProbeReturnsNull() {
        String url = "http://a/definition?k=SECRET";
        assertEquals(url, SessionDefinitionHttpsUpgrade.resolve(url, endpoint ->
            endpoint.startsWith("http://a") ? "{\"httpsBaseUrl\":\"https://a:8443\"}" : null));
    }

    @Test
    public void resolveLeavesUrlUnchangedWhenHttpsProbeReturnsDifferentBaseUrl() {
        String url = "http://a/definition?k=SECRET";
        assertEquals(url, SessionDefinitionHttpsUpgrade.resolve(url, endpoint ->
            endpoint.startsWith("http://")
                ? "{\"httpsBaseUrl\":\"https://a:8443\"}"
                : "{\"httpsBaseUrl\":\"https://b:8443\"}"));
    }

    @Test
    public void resolveLeavesUrlUnchangedWhenHttpsProbeThrowsIOException() {
        String url = "http://a/definition?k=SECRET";
        assertEquals(url, SessionDefinitionHttpsUpgrade.resolve(url, endpoint -> {
            if (endpoint.startsWith("https://")) throw new IOException("failure");
            return "{\"httpsBaseUrl\":\"https://a:8443\"}";
        }));
    }

    @Test
    public void resolveUpgradesWhenHttpsBaseUrlHasTrailingSlash() {
        assertEquals("https://a:8443/x?k=1",
            SessionDefinitionHttpsUpgrade.resolve("http://a:9980/x?k=1", endpoint -> {
                if (endpoint.equals("http://a:9980/api/console/https-endpoint")) {
                    return "{\"httpsBaseUrl\":\"https://a:8443/\"}";
                }
                if (endpoint.equals("https://a:8443/api/console/https-endpoint")) {
                    return "{\"httpsBaseUrl\":\"https://a:8443\"}";
                }
                return null;
            }));
    }

    @Test
    public void startGateAllowsOnlyFirstClaim() {
        SessionDefinitionHttpsUpgrade.StartGate startGate =
            new SessionDefinitionHttpsUpgrade.StartGate();
        assertTrue(startGate.tryClaim());
        assertFalse(startGate.tryClaim());
        assertFalse(startGate.tryClaim());
    }

    @Test
    public void startGatesAreIndependent() {
        SessionDefinitionHttpsUpgrade.StartGate firstStartGate =
            new SessionDefinitionHttpsUpgrade.StartGate();
        SessionDefinitionHttpsUpgrade.StartGate secondStartGate =
            new SessionDefinitionHttpsUpgrade.StartGate();
        assertTrue(firstStartGate.tryClaim());
        assertTrue(secondStartGate.tryClaim());
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

    @Test
    public void validatedHttpsBaseUrlRemovesTrailingSlash() {
        assertEquals("https://a:8443",
            SessionDefinitionHttpsUpgrade.validatedHttpsBaseUrl("http://a:9980", "https://a:8443/"));
    }

    @Test
    public void shouldPersistTrueWhenUpgradedAndUrlUnchangedDuringFetch() {
        assertTrue(SessionDefinitionHttpsUpgrade.shouldPersist(
            "http://a/x?k=1", "https://a:8443/x?k=1", "http://a/x?k=1"));
    }

    @Test
    public void shouldPersistFalseWhenUserChangedUrlDuringFetch() {
        assertFalse(SessionDefinitionHttpsUpgrade.shouldPersist(
            "http://a/x?k=1", "https://a:8443/x?k=1", "http://a/y?k=2"));
    }

    @Test
    public void shouldPersistFalseWhenNothingWasUpgraded() {
        assertFalse(SessionDefinitionHttpsUpgrade.shouldPersist(
            "http://a/x?k=1", "http://a/x?k=1", "http://a/x?k=1"));
    }

    @Test
    public void upgradeStoreSavesRewrittenUrlAndReturnsTrue() {
        InMemoryUrlStore store = new InMemoryUrlStore("http://a:9980/x?k=1");
        assertTrue(SessionDefinitionHttpsUpgrade.upgradeStore(store, endpoint ->
            httpsEndpointResponseForHost(endpoint, "a")));
        assertEquals("https://a:8443/x?k=1", store.get());
        assertEquals(1, store.setCalls);
    }

    @Test
    public void upgradeStoreDoesNotOverwriteUrlChangedDuringCheck() {
        InMemoryUrlStore store = new InMemoryUrlStore("http://a:9980/x?k=1");
        assertFalse(SessionDefinitionHttpsUpgrade.upgradeStore(store, endpoint -> {
            if (endpoint.startsWith("http://a:")) store.value = "http://a:9980/other?k=2";
            return httpsEndpointResponseForHost(endpoint, "a");
        }));
        assertEquals("http://a:9980/other?k=2", store.get());
        assertEquals(0, store.setCalls);
    }

    @Test
    public void upgradeStoreDoesNothingWhenNotUpgradable() {
        InMemoryUrlStore store = new InMemoryUrlStore("http://a:9980/x?k=1");
        assertFalse(SessionDefinitionHttpsUpgrade.upgradeStore(store, endpoint -> null));
        assertEquals("http://a:9980/x?k=1", store.get());
        assertEquals(0, store.setCalls);
    }

    @Test
    public void upgradeStoreDoesNothingForHttpsUrl() {
        InMemoryUrlStore store = new InMemoryUrlStore("https://a:8443/x");
        AtomicInteger calls = new AtomicInteger();
        assertFalse(SessionDefinitionHttpsUpgrade.upgradeStore(store, endpoint -> {
            calls.incrementAndGet();
            return "{\"httpsBaseUrl\":\"https://a:8443\"}";
        }));
        assertEquals("https://a:8443/x", store.get());
        assertEquals(0, store.setCalls);
        assertEquals(0, calls.get());
    }

    private static String httpsEndpointResponseForHost(String endpoint, String expectedHost) {
        try {
            URI uri = new URI(endpoint);
            return expectedHost.equals(uri.getHost())
                ? "{\"httpsBaseUrl\":\"https://a:8443\"}"
                : null;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static final class InMemoryUrlStore implements SessionDefinitionHttpsUpgrade.UrlStore {
        private String value;
        private int setCalls;

        private InMemoryUrlStore(String value) {
            this.value = value;
        }

        public String get() {
            return value;
        }

        public void set(String url) {
            value = url;
            setCalls++;
        }
    }

    private static void assertProductionResultUnchanged(int status, String responseBody, String location)
        throws IOException {
        LocalHttpServer server = startServer(status, responseBody, location);
        try {
            String url = "http://127.0.0.1:" + server.port() + "/s?k=SECRET";
            assertEquals(url,
                SessionDefinitionHttpsUpgrade.resolve(url, SessionDefinitionHttpsUpgrade.PRODUCTION_FETCHER));
        } finally {
            server.stop();
        }
    }

    private static LocalHttpServer startServer(int status, String responseBody, String location) throws IOException {
        LocalHttpServer server = new LocalHttpServer(status, responseBody, location);
        server.start();
        return server;
    }

    private static final class LocalRequest {
        private final String method;
        private final String target;
        private final Map<String, String> headers;

        private LocalRequest(String method, String target, Map<String, String> headers) {
            this.method = method;
            this.target = target;
            this.headers = headers;
        }
    }

    private static final class LocalHttpServer {
        private final int status;
        private final byte[] responseBody;
        private final String location;
        private final ServerSocket serverSocket;
        private final List<LocalRequest> requests = Collections.synchronizedList(new ArrayList<LocalRequest>());
        private Thread thread;

        private LocalHttpServer(int status, String responseBody, String location) throws IOException {
            this.status = status;
            this.responseBody = responseBody.getBytes("UTF-8");
            this.location = location;
            this.serverSocket = new ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"));
        }

        private void start() {
            thread = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        handle(serverSocket.accept());
                    } catch (IOException exception) {
                        if (!serverSocket.isClosed()) throw new RuntimeException(exception);
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        private void handle(Socket socket) throws IOException {
            try (Socket connection = socket) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String requestLine = reader.readLine();
                if (requestLine == null) return;
                String[] requestParts = requestLine.split(" ", 3);
                Map<String, String> headers = new java.util.HashMap<String, String>();
                String headerLine;
                while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                    int separator = headerLine.indexOf(':');
                    if (separator > 0) {
                        headers.put(headerLine.substring(0, separator).toLowerCase(Locale.US),
                            headerLine.substring(separator + 1).trim());
                    }
                }
                requests.add(new LocalRequest(requestParts[0], requestParts[1], headers));
                OutputStream output = connection.getOutputStream();
                StringBuilder responseHeaders = new StringBuilder();
                responseHeaders.append("HTTP/1.1 ").append(status).append(" OK\r\n");
                responseHeaders.append("Content-Length: ").append(responseBody.length).append("\r\n");
                if (location != null) responseHeaders.append("Location: ").append(location).append("\r\n");
                responseHeaders.append("Connection: close\r\n\r\n");
                output.write(responseHeaders.toString().getBytes("UTF-8"));
                output.write(responseBody);
                output.flush();
            }
        }

        private int port() {
            return serverSocket.getLocalPort();
        }

        private List<LocalRequest> requests() {
            return requests;
        }

        private void stop() throws IOException {
            serverSocket.close();
            if (thread != null) {
                try {
                    thread.join(1000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException(exception);
                }
            }
        }
    }
}
