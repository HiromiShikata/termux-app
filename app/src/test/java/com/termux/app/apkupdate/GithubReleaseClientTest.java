package com.termux.app.apkupdate;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GithubReleaseClientTest {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private final GithubReleaseClient client = new GithubReleaseClient();

    @Before
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"));
    }

    @After
    public void stopServer() throws IOException {
        if (serverThread != null) {
            serverThread.interrupt();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + serverSocket.getLocalPort() + "/release";
    }

    private void serveOnce(int statusCode, String reasonPhrase, String body) {
        serverThread = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    // Consume request headers until the blank line.
                }
                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                StringBuilder header = new StringBuilder();
                header.append("HTTP/1.1 ").append(statusCode).append(' ').append(reasonPhrase).append("\r\n");
                header.append("Content-Type: application/json; charset=utf-8\r\n");
                header.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
                header.append("Connection: close\r\n\r\n");
                OutputStream out = socket.getOutputStream();
                out.write(header.toString().getBytes(StandardCharsets.UTF_8));
                out.write(bodyBytes);
                out.flush();
            } catch (IOException ignored) {
                // Test teardown closes the socket; ignore the resulting exception.
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Test
    public void returnsResponseBodyOnHttpOk() throws IOException {
        String json = "{\"tag_name\":\"v0.119.0\"}";
        serveOnce(200, "OK", json);

        String result = client.fetchLatestReleaseJson(baseUrl());

        Assert.assertEquals(json, result);
    }

    @Test
    public void readsMultiByteUtf8BodyCorrectly() throws IOException {
        String json = "{\"tag_name\":\"v0.119.0\",\"note\":\"cafe — naïve\"}";
        serveOnce(200, "OK", json);

        String result = client.fetchLatestReleaseJson(baseUrl());

        Assert.assertEquals(json, result);
    }

    @Test
    public void readsLargeBodyExceedingSingleBufferChunk() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < 5000; index++) {
            builder.append("0123456789");
        }
        String body = builder.toString();
        serveOnce(200, "OK", body);

        String result = client.fetchLatestReleaseJson(baseUrl());

        Assert.assertEquals(50000, result.length());
        Assert.assertEquals(body, result);
    }

    @Test
    public void throwsWhenServerReturnsNonOkStatus() {
        serveOnce(404, "Not Found", "");

        IOException thrown =
            Assert.assertThrows(IOException.class, () -> client.fetchLatestReleaseJson(baseUrl()));
        Assert.assertTrue(thrown.getMessage().contains("404"));
    }

    @Test
    public void throwsForUnsupportedProtocol() {
        IOException thrown = Assert.assertThrows(IOException.class,
            () -> client.fetchLatestReleaseJson("file:///tmp/does-not-matter.json"));
        Assert.assertTrue(thrown.getMessage().contains("Unsupported protocol"));
    }
}
