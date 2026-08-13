package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class OwnerCallDialogDeviceScreenshotInstrumentedTest {

    private static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";
    private static final String QUIET_SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1885";
    private static final String OLDEST_CALL_BODY =
        "クロネコ住所録の旧住所の一括削除を実行してよいかご判断ください";
    private static final String REPEATED_CALL_BODY = "請求書の送付先を変更してよいかご判断ください";
    private static final String DETACHED_SESSION_SHELL = "/system/bin/sh";
    private static final long ENTRY_LOAD_TIMEOUT_MILLIS = 20_000L;
    private static final String PROJECT_DOCUMENT = "{"
        + "\"version\":5,"
        + "\"tdpmConsoleUrl\":\"http://127.0.0.1/\","
        + "\"newIssueUrl\":\"https://github.com/HiromiShikata/termux-app/issues/new\","
        + "\"groups\":[{\"story\":\"owner call dialog\",\"sessions\":["
        + "{\"name\":\"" + SESSION_URL + "\",\"description\":\"waiting on the owner\","
        + "\"unansweredCalls\":["
        + "{\"calledAt\":\"2026-08-13T00:00:00.000Z\",\"body\":\"" + OLDEST_CALL_BODY + "\"},"
        + "{\"calledAt\":\"2026-08-13T00:03:00.000Z\",\"body\":\"" + REPEATED_CALL_BODY + "\"},"
        + "{\"calledAt\":\"2026-08-13T00:05:00.000Z\",\"body\":\"" + REPEATED_CALL_BODY + "\"}]},"
        + "{\"name\":\"" + QUIET_SESSION_URL + "\",\"description\":\"no waiting call\","
        + "\"unansweredCalls\":[]}]}]}";

    private LocalDocumentServer documentServer;
    private String previousSessionDefinitionUrl;

    @Before
    public void startTheDocumentServer() throws IOException {
        previousSessionDefinitionUrl = preferences().getSessionDefinitionUrl();
        documentServer = new LocalDocumentServer();
        documentServer.start();
    }

    @After
    public void stopTheDocumentServer() {
        preferences().setSessionDefinitionUrl(previousSessionDefinitionUrl);
        if (documentServer != null) {
            documentServer.stop();
        }
    }

    private static TermuxAppSharedPreferences preferences() {
        return TermuxAppSharedPreferences.build(
            InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void theWaitingOwnerCallStaysReadableOverTheTerminalOnTheDevice() throws Exception {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            activity.getPreferences().setSessionDefinitionUrl(documentServer.indexUrl());
            activity.loadSessionsFromDefinition();
        });

        awaitLoadedEntries(scenario);

        scenario.onActivity(activity -> {
            displaySession(activity, SESSION_URL);
            activity.showUnansweredOwnerCallsOfDisplayedSession();

            View dialog = activity.findViewById(R.id.owner_call_dialog);
            assertNotNull(dialog);
            assertEquals(View.VISIBLE, dialog.getVisibility());
            assertEquals("1 / 3", textOf(activity, R.id.owner_call_dialog_position));
            assertEquals(OLDEST_CALL_BODY, textOf(activity, R.id.owner_call_dialog_body));
            assertTrue("the elapsed time since the owner was called must be shown",
                textOf(activity, R.id.owner_call_dialog_relative_time).endsWith("前"));
            assertEquals(activity.getResources().getDisplayMetrics().heightPixels
                    / OwnerCallDialogGeometry.SCREEN_HEIGHT_DIVISOR,
                dialog.getLayoutParams().height);
        });

        File screenshot = takeScreenshot("owner-call-dialog-device.png");
        assertTrue("a device screenshot of the dialog must be captured",
            screenshot.exists() && screenshot.length() > 0);

        scenario.onActivity(activity -> {
            activity.findViewById(R.id.owner_call_dialog_next_button).performClick();
            assertEquals("2 / 3", textOf(activity, R.id.owner_call_dialog_position));
            assertEquals(REPEATED_CALL_BODY, textOf(activity, R.id.owner_call_dialog_body));

            activity.findViewById(R.id.owner_call_dialog_close_button).performClick();
            assertEquals("1 / 2", textOf(activity, R.id.owner_call_dialog_position));

            displaySession(activity, QUIET_SESSION_URL);
            activity.showUnansweredOwnerCallsOfDisplayedSession();
            assertEquals(View.GONE,
                activity.findViewById(R.id.owner_call_dialog).getVisibility());
        });
        scenario.close();
    }

    private void awaitLoadedEntries(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        long deadline = System.currentTimeMillis() + ENTRY_LOAD_TIMEOUT_MILLIS;
        boolean[] loaded = new boolean[1];
        while (System.currentTimeMillis() < deadline && !loaded[0]) {
            scenario.onActivity(activity -> loaded[0] = !activity.getSessionDefinitionEntries().isEmpty());
            if (!loaded[0]) {
                Thread.sleep(200L);
            }
        }
        assertTrue("the session definition document served over http must reach the app", loaded[0]);
    }

    private static void displaySession(TermuxActivity activity, String sessionName) {
        TerminalView terminalView = activity.getTerminalView();
        assertNotNull(terminalView);
        TerminalSession session = new TerminalSession(DETACHED_SESSION_SHELL, "/",
            new String[]{DETACHED_SESSION_SHELL}, new String[0], null,
            new TermuxTerminalSessionClientBase());
        session.mSessionName = sessionName;
        terminalView.mTermSession = session;
    }

    private static String textOf(Activity activity, int viewId) {
        return ((TextView) activity.findViewById(viewId)).getText().toString();
    }

    private static File takeScreenshot(String fileName) {
        File output = new File(
            InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),
            fileName);
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(output);
        return output;
    }

    private static final class LocalDocumentServer {

        private ServerSocket serverSocket;
        private Thread acceptThread;
        private volatile boolean running;

        void start() throws IOException {
            serverSocket = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"));
            running = true;
            acceptThread = new Thread(this::acceptRequests);
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        String indexUrl() {
            return "http://127.0.0.1:" + serverSocket.getLocalPort() + "/index.v5.json";
        }

        private void acceptRequests() {
            while (running) {
                try (Socket socket = serverSocket.accept()) {
                    String requestLine = readRequestLine(socket);
                    respond(socket, requestLine.contains("/index.v5.json")
                        ? "{\"version\":5,\"projects\":[{\"name\":\"demo\",\"path\":\"/demo.v5.json\"}]}"
                        : PROJECT_DOCUMENT);
                } catch (IOException closedWhileStopping) {
                    if (running) {
                        throw new IllegalStateException(closedWhileStopping);
                    }
                }
            }
        }

        private static String readRequestLine(Socket socket) throws IOException {
            StringBuilder line = new StringBuilder();
            int character;
            while ((character = socket.getInputStream().read()) != -1 && character != '\n') {
                line.append((char) character);
            }
            return line.toString();
        }

        private static void respond(Socket socket, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            OutputStream output = socket.getOutputStream();
            output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.flush();
        }

        void stop() {
            running = false;
            try {
                serverSocket.close();
            } catch (IOException alreadyClosed) {
                throw new IllegalStateException(alreadyClosed);
            }
        }
    }
}
