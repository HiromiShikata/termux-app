package com.termux.app.ownercall;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HttpOwnerCallFileTransportTest {

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String OWNER_CALL_FILE =
        "---\nsessionName: \"secretary\"\ncalledAt: \"2026-08-14T04:22:28Z\"\n"
            + "body: |2\n  Decide whether the previous addresses may be deleted in bulk.\n";

    private AnsweringServer server;
    private final HttpOwnerCallFileTransport transport = new HttpOwnerCallFileTransport();

    @Before
    public void startTheServer() throws IOException {
        server = new AnsweringServer();
        server.start();
    }

    @After
    public void stopTheServer() throws IOException, InterruptedException {
        server.stop();
    }

    @Test
    public void readsTheOwnerCallFileTheServerHolds() throws IOException {
        server.answerWith(200, OWNER_CALL_FILE);

        Assert.assertEquals(OWNER_CALL_FILE, transport.fetch(server.url()));
    }

    @Test
    public void readsNothingWhenTheServerDoesNotHoldTheFile() throws IOException {
        server.answerWith(404, "");

        Assert.assertEquals("", transport.fetch(server.url()));
    }

    @Test
    public void reportsThatTheFileCannotBeReadWhenTheServerRejectsTheAccessToken() {
        server.answerWith(401, "");

        IOException unreadableFile = Assert.assertThrows(IOException.class,
            () -> transport.fetch(server.url()));

        assertCarriesNoAccessToken(unreadableFile);
    }

    @Test
    public void reportsThatTheFileCannotBeReadWhenTheServerFails() {
        server.answerWith(500, "");

        IOException unreadableFile = Assert.assertThrows(IOException.class,
            () -> transport.fetch(server.url()));

        assertCarriesNoAccessToken(unreadableFile);
    }

    @Test
    public void reportsThatTheFileCannotBeReadWhenNothingAnswers() throws IOException {
        String urlOfAPortNothingEverListenedOn = ownerCallFileUrlOfPort(portNothingListensOn());

        IOException unreachableServer = Assert.assertThrows(IOException.class,
            () -> transport.fetch(urlOfAPortNothingEverListenedOn));

        assertCarriesNoAccessToken(unreachableServer);
    }

    @Test
    public void reportsThatTheFileCannotBeReadOverAProtocolItDoesNotSpeak() {
        IOException unusableUrl = Assert.assertThrows(IOException.class, () -> transport.fetch(
            "ftp://127.0.0.1/in-tmux-by-human/call-to-user/NA/secretary.yaml?k=" + ACCESS_TOKEN));

        assertCarriesNoAccessToken(unusableUrl);
    }

    @Test
    public void deletesTheFileTheOwnerAnswered() throws IOException {
        server.answerWith(204, "");

        transport.delete(server.url());

        Assert.assertEquals(Collections.singletonList("DELETE"), server.receivedMethods());
    }

    @Test
    public void treatsAnAlreadyDeletedFileAsDeleted() throws IOException {
        server.answerWith(404, "");

        transport.delete(server.url());
    }

    @Test
    public void reportsThatTheFileCannotBeDeletedWhenTheServerFails() {
        server.answerWith(500, "");

        IOException undeletableFile = Assert.assertThrows(IOException.class,
            () -> transport.delete(server.url()));

        assertCarriesNoAccessToken(undeletableFile);
    }

    private static void assertCarriesNoAccessToken(IOException failure) {
        Assert.assertFalse("a failure the app logs must not carry the access token: "
            + failure.getMessage(), failure.getMessage().contains(ACCESS_TOKEN));
    }

    private static int portNothingListensOn() throws IOException {
        try (ServerSocket probe = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
            return probe.getLocalPort();
        }
    }

    private static String ownerCallFileUrl(ServerSocket serverSocket) {
        return ownerCallFileUrlOfPort(serverSocket.getLocalPort());
    }

    private static String ownerCallFileUrlOfPort(int port) {
        return "http://127.0.0.1:" + port
            + "/in-tmux-by-human/call-to-user/NA/secretary.yaml?k=" + ACCESS_TOKEN;
    }

    private static final class AnsweringServer {

        private static final long STOP_TIMEOUT_MILLIS = 5000L;

        private ServerSocket serverSocket;
        private Thread acceptThread;
        private volatile int statusCode = 200;
        private volatile String body = "";
        private final List<String> receivedMethods = Collections.synchronizedList(new ArrayList<>());

        void start() throws IOException {
            serverSocket = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
            acceptThread = new Thread(this::acceptRequests);
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        void answerWith(int answeredStatusCode, String answeredBody) {
            statusCode = answeredStatusCode;
            body = answeredBody;
        }

        String url() {
            return ownerCallFileUrl(serverSocket);
        }

        List<String> receivedMethods() {
            return new ArrayList<>(receivedMethods);
        }

        private void acceptRequests() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    receivedMethods.add(readRequestHead(socket).split(" ", 2)[0]);
                    answer(socket);
                } catch (IOException closedWhileStopping) {
                    return;
                }
            }
        }

        private void answer(Socket socket) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            OutputStream output = socket.getOutputStream();
            output.write(("HTTP/1.1 " + statusCode + " \r\nContent-Type: text/plain\r\n"
                + "Content-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.flush();
            socket.shutdownOutput();
        }

        private static String readRequestHead(Socket socket) throws IOException {
            StringBuilder head = new StringBuilder();
            InputStream input = socket.getInputStream();
            int character;
            while ((character = input.read()) != -1) {
                head.append((char) character);
                if (head.length() >= 4 && head.substring(head.length() - 4).equals("\r\n\r\n")) {
                    break;
                }
            }
            return head.toString();
        }

        void stop() throws IOException, InterruptedException {
            serverSocket.close();
            acceptThread.join(STOP_TIMEOUT_MILLIS);
            if (acceptThread.isAlive()) {
                throw new IllegalStateException("the local server kept accepting requests after "
                    + "it was stopped");
            }
        }
    }
}
