package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class PerOutputStatuslineScanReadsTheVisibleScreenTest {

    private static final String SESSION_NAME = "session-rendering-a-statusline";

    private static final int TRANSCRIPT_ROWS = 2000;

    private static final int SCREEN_COLUMNS = 80;

    private static final int SCREEN_ROWS = 24;

    private static final int SCROLLBACK_FILLER_ROWS = 400;

    private static final int VISIBLE_SCREEN_CHARACTER_BUDGET = SCREEN_ROWS * (SCREEN_COLUMNS + 1);

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TerminalSession session;

    @Before
    public void setUp() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();
        set(activity, "mProperties", TermuxAppSharedProperties.init(appContext));
        sessionActivityClient = new TermuxTerminalSessionActivityClient(activity);
        session = new TerminalSession("/system/bin/sh", "/", new String[0], new String[0],
            TRANSCRIPT_ROWS, sessionActivityClient);
        session.mSessionName = SESSION_NAME;
        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
            () -> session.initializeEmulator(SCREEN_COLUMNS, SCREEN_ROWS, 10, 20));
        renderAScrollbackMuchLargerThanTheScreen();
    }

    @Test
    public void oneOutputEventBuildsItsStatuslineScanTextFromTheVisibleScreenOnly() {
        TermuxTerminalSessionActivityClient.SessionOutputScanText scanText =
            sessionActivityClient.readOutputScanTextOnce(session);
        assertNotNull(scanText);

        int statuslineScanCharacters = scanText.getStatuslineScanText().length();

        assertTrue("the statusline a Claude Code session renders is on the visible screen, so the "
                + "per-output statusline parse needs the visible screen and nothing more. Building "
                + "that text from the whole scrollback allocates and copies a string the size of the "
                + "scrollback on the thread that draws, for every single output event, on top of the "
                + "one transcript read the same method already performs. The visible screen of this "
                + "session is at most " + VISIBLE_SCREEN_CHARACTER_BUDGET + " characters, yet the "
                + "statusline scan text came to " + statuslineScanCharacters,
            statuslineScanCharacters <= VISIBLE_SCREEN_CHARACTER_BUDGET);
    }

    @Test
    public void theOutputTagScanKeepsReceivingTheWholeScrollback() {
        TermuxTerminalSessionActivityClient.SessionOutputScanText scanText =
            sessionActivityClient.readOutputScanTextOnce(session);
        assertNotNull(scanText);

        int transcriptCharacters = scanText.getTranscriptText().length();

        assertTrue("the output tag scanner scans the whole scrollback by design, so narrowing the "
                + "statusline scan text must not narrow the transcript this carrier also holds. The "
                + "rendered scrollback is far larger than the visible screen budget of "
                + VISIBLE_SCREEN_CHARACTER_BUDGET + " characters, yet the transcript came to "
                + transcriptCharacters,
            transcriptCharacters > VISIBLE_SCREEN_CHARACTER_BUDGET);
    }

    private static void set(TermuxActivity activity, String fieldName, Object value)
            throws Exception {
        Field field = TermuxActivity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(activity, value);
    }

    private void renderAScrollbackMuchLargerThanTheScreen() {
        StringBuilder rendered = new StringBuilder();
        for (int row = 0; row < SCROLLBACK_FILLER_ROWS; row++) {
            rendered.append("scrollback row ").append(row).append(" of ").append(SESSION_NAME)
                .append("\r\n");
        }
        byte[] renderedBytes = rendered.toString().getBytes(StandardCharsets.UTF_8);
        session.getEmulator().append(renderedBytes, renderedBytes.length);
    }
}
