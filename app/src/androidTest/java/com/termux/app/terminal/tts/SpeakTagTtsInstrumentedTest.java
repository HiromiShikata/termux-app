package com.termux.app.terminal.tts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.graphics.Bitmap;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class SpeakTagTtsInstrumentedTest {

    private static final String SHELL = "/system/bin/sh";

    private static final String SESSION_OUTPUT = "\r\n<speak>hello world</speak>\r\n";

    @Test
    public void speakTagInSessionOutputIsStrippedFromScreenAndInnerTextRemainsVisible() throws Exception {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);

        AtomicReference<TerminalSession> sessionRef = new AtomicReference<>();
        scenario.onActivity(activity -> {
            activity.getPreferences().setSpeakTagAutoReadEnabled(true);
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);
            assertNotNull(activity.getTtsManager());

            TerminalView terminalView = activity.getTerminalView();
            TerminalSession session = new TerminalSession(
                SHELL, "/", new String[]{SHELL}, new String[0], null, client);
            terminalView.attachSession(session);
            sessionRef.set(session);
        });

        TerminalSession session = sessionRef.get();
        assertNotNull(session);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            byte[] output = SESSION_OUTPUT.getBytes(StandardCharsets.UTF_8);
            session.getEmulator().append(output, output.length);
        });

        String transcript = waitForTranscriptContaining(session, "hello world", 5000L);

        assertTrue("inner text should remain visible, transcript=[" + transcript + "]",
            transcript.contains("hello world"));
        assertFalse("open marker should be stripped, transcript=[" + transcript + "]",
            transcript.contains("<speak>"));
        assertFalse("close marker should be stripped, transcript=[" + transcript + "]",
            transcript.contains("</speak>"));

        captureScreenshot();

        scenario.close();
    }

    private static String waitForTranscriptContaining(TerminalSession session, String needle, long timeoutMillis)
        throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        String transcript = "";
        while (System.currentTimeMillis() < deadline) {
            transcript = readTranscript(session);
            if (transcript.contains(needle)) return transcript;
            Thread.sleep(100L);
        }
        return transcript;
    }

    private static String readTranscript(TerminalSession session) {
        AtomicReference<String> ref = new AtomicReference<>("");
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            if (session.getEmulator() != null && session.getEmulator().getScreen() != null) {
                ref.set(session.getEmulator().getScreen().getTranscriptText());
            }
        });
        return ref.get();
    }

    private static void captureScreenshot() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        if (bitmap == null) return;
        File directory = instrumentation.getTargetContext().getExternalFilesDir(null);
        File file = new File(directory, "speak-tag-terminal.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
