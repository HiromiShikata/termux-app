package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HttpUrlLiveToolCallHttpClientTest {

    private static final String PATH = "/api/live/tool-calls";
    private static final String REQUEST_BODY =
        "{\"name\":\"example_read_tool\",\"arguments\":{\"limit\":3},\"sessionName\":\"alpha\"}";
    private static final String OK_BODY = "{\"output\":\"ok\"}";

    private LocalHttpServer mServer;

    @Before
    public void startServer() throws IOException {
        mServer = new LocalHttpServer();
    }

    @After
    public void stopServer() throws Exception {
        mServer.close();
    }

    private String endpoint() {
        return "http://127.0.0.1:" + mServer.port() + PATH;
    }

    @Test
    public void postsJsonBodyVerbatimAndReturnsOkStatusWithBody() throws Exception {
        mServer.reply(200, OK_BODY);
        LiveSessionHttpResponse response = new HttpUrlLiveToolCallHttpClient().post(endpoint(), REQUEST_BODY);
        assertEquals("POST", mServer.lastMethod());
        assertEquals("application/json", mServer.lastContentType());
        assertEquals(REQUEST_BODY, mServer.lastBody());
        assertEquals(200, response.getStatusCode());
        assertEquals(OK_BODY, response.getBody());
    }

    @Test
    public void returnsClientErrorStatusAndErrorBody() throws Exception {
        int[] statuses = {400, 404};
        for (int status : statuses) {
            mServer.reply(status, "{\"error\":\"rejected\"}");
            LiveSessionHttpResponse response = new HttpUrlLiveToolCallHttpClient().post(endpoint(), REQUEST_BODY);
            assertEquals(status, response.getStatusCode());
            assertEquals("{\"error\":\"rejected\"}", response.getBody());
        }
    }

    @Test
    public void returnsServerErrorStatusAndErrorBody() throws Exception {
        int[] statuses = {500, 503};
        for (int status : statuses) {
            mServer.reply(status, "internal failure");
            LiveSessionHttpResponse response = new HttpUrlLiveToolCallHttpClient().post(endpoint(), REQUEST_BODY);
            assertEquals(status, response.getStatusCode());
            assertEquals("internal failure", response.getBody());
        }
    }

    @Test
    public void serverErrorWithNoBodyReturnsEmptyBodyWithoutException() throws Exception {
        mServer.reply(500, "");
        LiveSessionHttpResponse response = new HttpUrlLiveToolCallHttpClient().post(endpoint(), REQUEST_BODY);
        assertEquals(500, response.getStatusCode());
        assertEquals("", response.getBody());
    }

    @Test
    public void refusedConnectionFailsWithConnectException() throws Exception {
        int closedPort;
        try (ServerSocket probe = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            closedPort = probe.getLocalPort();
        }
        try {
            new HttpUrlLiveToolCallHttpClient().post("http://127.0.0.1:" + closedPort + PATH, REQUEST_BODY);
            fail("expected the refused connection to fail");
        } catch (ConnectException expected) {
            assertEquals(ConnectException.class, expected.getClass());
        }
    }

    @Test
    public void responseSlowerThanReadTimeoutFailsWithSocketTimeout() throws Exception {
        mServer.hangWithoutResponse();
        try {
            new HttpUrlLiveToolCallHttpClient(200).post(endpoint(), REQUEST_BODY);
            fail("expected the silent server to time out");
        } catch (SocketTimeoutException expected) {
            assertEquals(SocketTimeoutException.class, expected.getClass());
        }
    }

    private static final class LocalHttpServer implements Closeable {

        private final ServerSocket mSocket;
        private final Thread mAcceptor;
        private final CountDownLatch mRelease = new CountDownLatch(1);
        private volatile String mMethod = "";
        private volatile String mContentType = "";
        private volatile String mBody = "";
        private volatile int mStatus = 200;
        private volatile String mReplyBody = "";
        private volatile boolean mHang;
        private volatile Throwable mFailure;

        LocalHttpServer() throws IOException {
            mSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            mAcceptor = new Thread(this::acceptLoop, "local-http-server");
            mAcceptor.setDaemon(true);
            mAcceptor.start();
        }

        int port() {
            return mSocket.getLocalPort();
        }

        void reply(int status, String body) {
            mStatus = status;
            mReplyBody = body;
            mHang = false;
        }

        void hangWithoutResponse() {
            mHang = true;
        }

        String lastMethod() {
            return mMethod;
        }

        String lastContentType() {
            return mContentType;
        }

        String lastBody() {
            return mBody;
        }

        private void acceptLoop() {
            while (!mSocket.isClosed()) {
                try (Socket connection = mSocket.accept()) {
                    handle(connection);
                } catch (IOException acceptOrRequestFailure) {
                    if (!mSocket.isClosed()) {
                        mFailure = acceptOrRequestFailure;
                    }
                }
            }
        }

        private void handle(Socket connection) throws IOException {
            InputStream input = connection.getInputStream();
            String requestLine = readLine(input);
            mMethod = requestLine.substring(0, requestLine.indexOf(' '));
            int contentLength = 0;
            String contentType = "";
            String header = readLine(input);
            while (!header.isEmpty()) {
                int colon = header.indexOf(':');
                String name = header.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = header.substring(colon + 1).trim();
                if (name.equals("content-type")) {
                    contentType = value;
                } else if (name.equals("content-length")) {
                    contentLength = Integer.parseInt(value);
                }
                header = readLine(input);
            }
            mContentType = contentType;
            mBody = new String(readExactly(input, contentLength), StandardCharsets.UTF_8);
            if (mHang) {
                awaitRelease();
                return;
            }
            writeReply(connection.getOutputStream());
        }

        private void awaitRelease() {
            try {
                mRelease.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        private void writeReply(OutputStream output) throws IOException {
            byte[] body = mReplyBody.getBytes(StandardCharsets.UTF_8);
            String head = "HTTP/1.1 " + mStatus + " " + reasonPhrase(mStatus) + "\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
            output.write(head.getBytes(StandardCharsets.ISO_8859_1));
            output.write(body);
            output.flush();
        }

        private static String reasonPhrase(int status) {
            switch (status) {
                case 200:
                    return "OK";
                case 400:
                    return "Bad Request";
                case 404:
                    return "Not Found";
                case 500:
                    return "Internal Server Error";
                case 503:
                    return "Service Unavailable";
                default:
                    throw new IllegalArgumentException("no reason phrase configured for " + status);
            }
        }

        private static String readLine(InputStream input) throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int next;
            while ((next = input.read()) != -1 && next != '\n') {
                if (next != '\r') {
                    line.write(next);
                }
            }
            return line.toString(StandardCharsets.ISO_8859_1.name());
        }

        private static byte[] readExactly(InputStream input, int length) throws IOException {
            byte[] bytes = new byte[length];
            int filled = 0;
            while (filled < length) {
                int read = input.read(bytes, filled, length - filled);
                if (read == -1) {
                    throw new IOException("request ended after " + filled + " of " + length + " body bytes");
                }
                filled += read;
            }
            return bytes;
        }

        @Override
        public void close() throws IOException {
            mRelease.countDown();
            mSocket.close();
            try {
                mAcceptor.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            if (mFailure != null) {
                throw new IOException("the local test server failed", mFailure);
            }
        }
    }
}
