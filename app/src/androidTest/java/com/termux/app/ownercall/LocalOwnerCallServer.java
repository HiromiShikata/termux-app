package com.termux.app.ownercall;

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
import java.util.concurrent.atomic.AtomicReference;

/**
 * The console host as the app sees it over HTTP: the session definition documents and one session's
 * owner call file, served on loopback so a device test can drive the real network path the app uses
 * and observe the DELETE the app issues once the owner answers.
 */
public final class LocalOwnerCallServer {

    public static final String PROJECT_CODE = "umino";
    public static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";
    public static final String OWNER_CALL_FILE_PATH = "/in-tmux-by-human/call-to-user/umino/"
        + "https___github_com_HiromiShikata_termux-app_issues_1884.yaml";
    public static final String INDEX_PATH = "/in-tmux-by-human/index.v4.json";
    public static final String PROJECT_PATH = "/in-tmux-by-human/umino.v4.json";
    public static final String TAPPED_PAGE_PATH = "/the-page-a-tapped-url-opens";
    public static final String ACCESS_TOKEN = "device-test-token";

    private static final String TAPPED_PAGE_DOCUMENT = "the page a tapped url opens";

    private static final String INDEX_DOCUMENT = "{\"version\":4,\"projects\":[{\"name\":\""
        + PROJECT_CODE + "\",\"path\":\"" + PROJECT_PATH + "?k=" + ACCESS_TOKEN + "\"}]}";
    private static final String PROJECT_DOCUMENT = "{\"version\":4,"
        + "\"groups\":[{\"story\":\"owner call dialog\",\"sessions\":[{\"name\":\"" + SESSION_URL
        + "\",\"description\":\"waiting on the owner\"}]}]}";

    private static final long STOP_TIMEOUT_MILLIS = 5000L;

    private static final AtomicReference<LocalOwnerCallServer> RUNNING_SERVER =
        new AtomicReference<>();

    public static LocalOwnerCallServer runningServer() {
        return RUNNING_SERVER.get();
    }

    public static String ownerCallDocument(String calledAt, String body) {
        return "---\nsessionName: \"https_//github_com/HiromiShikata/termux-app/issues/1884\"\n"
            + "calledAt: \"" + calledAt + "\"\nbody: |2\n  " + body + "\n";
    }

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;
    private volatile IOException failure;
    private volatile boolean ownerCallFileExists = true;
    private volatile String ownerCallFile = "";
    private final List<String> deletedPaths = Collections.synchronizedList(new ArrayList<>());

    public LocalOwnerCallServer(String ownerCallFile) {
        this.ownerCallFile = ownerCallFile;
    }

    public void serveOwnerCallFile(String file) {
        ownerCallFile = file;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
        running = true;
        RUNNING_SERVER.set(this);
        acceptThread = new Thread(this::acceptRequests);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public String indexUrl() {
        return "http://127.0.0.1:" + serverSocket.getLocalPort() + INDEX_PATH
            + "?k=" + ACCESS_TOKEN;
    }

    public String tappedPageUrl() {
        return "http://127.0.0.1:" + serverSocket.getLocalPort() + TAPPED_PAGE_PATH;
    }

    public List<String> deletedPaths() {
        return new ArrayList<>(deletedPaths);
    }

    public boolean holdsTheOwnerCallFile() {
        return ownerCallFileExists;
    }

    public String describeFailure() {
        return failure == null ? "no error" : failure.toString();
    }

    private void acceptRequests() {
        while (running) {
            try (Socket socket = serverSocket.accept()) {
                respondTo(socket, readRequestHead(socket));
            } catch (IOException closedWhileStopping) {
                if (running) {
                    failure = closedWhileStopping;
                    return;
                }
            }
        }
    }

    private void respondTo(Socket socket, String requestHead) throws IOException {
        String requestLine = requestHead.split("\r\n", 2)[0];
        String[] requestLineParts = requestLine.split(" ");
        String method = requestLineParts.length > 0 ? requestLineParts[0] : "";
        String target = requestLineParts.length > 1 ? requestLineParts[1] : "";
        String path = target.split("\\?", 2)[0];
        if (TAPPED_PAGE_PATH.equals(path)) {
            respond(socket, 200, TAPPED_PAGE_DOCUMENT);
            return;
        }
        if (!target.contains("k=" + ACCESS_TOKEN)) {
            respond(socket, 401, "");
            return;
        }
        if (OWNER_CALL_FILE_PATH.equals(path) && "DELETE".equals(method)) {
            deletedPaths.add(path);
            ownerCallFileExists = false;
            respond(socket, 204, "");
            return;
        }
        if (OWNER_CALL_FILE_PATH.equals(path)) {
            respond(socket, ownerCallFileExists ? 200 : 404,
                ownerCallFileExists ? ownerCallFile : "");
            return;
        }
        if (INDEX_PATH.equals(path)) {
            respond(socket, 200, INDEX_DOCUMENT);
            return;
        }
        if (PROJECT_PATH.equals(path)) {
            respond(socket, 200, PROJECT_DOCUMENT);
            return;
        }
        respond(socket, 404, "");
    }

    private static String readRequestHead(Socket socket) throws IOException {
        StringBuilder head = new StringBuilder();
        InputStream input = socket.getInputStream();
        int character;
        while ((character = input.read()) != -1) {
            head.append((char) character);
            if (endsWithBlankLine(head)) {
                break;
            }
        }
        return head.toString();
    }

    private static boolean endsWithBlankLine(StringBuilder head) {
        int length = head.length();
        if (length >= 2 && head.charAt(length - 2) == '\n' && head.charAt(length - 1) == '\n') {
            return true;
        }
        return length >= 4 && head.charAt(length - 4) == '\r' && head.charAt(length - 3) == '\n'
            && head.charAt(length - 2) == '\r' && head.charAt(length - 1) == '\n';
    }

    private static void respond(Socket socket, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        OutputStream output = socket.getOutputStream();
        output.write(("HTTP/1.1 " + statusCode + " \r\nContent-Type: text/plain\r\n"
            + "Content-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.flush();
        socket.shutdownOutput();
    }

    public void stop() throws InterruptedException {
        running = false;
        RUNNING_SERVER.set(null);
        try {
            serverSocket.close();
        } catch (IOException alreadyClosed) {
            throw new IllegalStateException(alreadyClosed);
        }
        acceptThread.join(STOP_TIMEOUT_MILLIS);
    }
}
