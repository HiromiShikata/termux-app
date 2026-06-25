package com.termux.app.terminal;

import com.termux.app.TermuxService;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class GenuineOutputActivitySignalTest {

    private static final String SESSION_NAME = "worker";

    private ServiceController<TermuxService> controller;
    private TermuxService service;
    private TermuxTerminalSessionServiceClient client;

    @Before
    public void setUp() {
        TermuxShellManager.init(RuntimeEnvironment.getApplication());
        controller = Robolectric.buildService(TermuxService.class).create();
        service = controller.get();
        client = new TermuxTerminalSessionServiceClient(service);
    }

    private static final class NoOpTerminalOutput extends TerminalOutput {
        @Override
        public void write(byte[] data, int offset, int count) {
        }

        @Override
        public void titleChanged(String oldTitle, String newTitle) {
        }

        @Override
        public void onCopyTextToClipboard(String text) {
        }

        @Override
        public void onPasteTextFromClipboard() {
        }

        @Override
        public void onBell() {
        }

        @Override
        public void onSpeakNotification(String text) {
        }

        @Override
        public void onColorsChanged() {
        }
    }

    private static TerminalEmulator newEmulator() {
        return new TerminalEmulator(new NoOpTerminalOutput(), 80, 24, 13, 15, 1000, null);
    }

    private static TerminalSession sessionWithEmulator(TerminalEmulator emulator) throws Exception {
        TerminalSession session = new TerminalSession(
            "/system/bin/sh", "/", new String[0], new String[0], 100, null);
        session.mSessionName = SESSION_NAME;
        Field emulatorField = TerminalSession.class.getDeclaredField("mEmulator");
        emulatorField.setAccessible(true);
        emulatorField.set(session, emulator);
        return session;
    }

    private static void emitGenuineOutput(TerminalEmulator emulator, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        emulator.appendGenuineOutput(bytes, 0, bytes.length);
    }

    private static void fillScreen(TerminalEmulator emulator) {
        for (int row = 0; row < 24; row++) {
            emitGenuineOutput(emulator, "row " + row + "\r\n");
        }
    }

    private Long lastOutputActivity() {
        return service.getSessionNewActivityStore().getLastOutputActivityTimeMillis(SESSION_NAME);
    }

    @Test
    public void newLinesThatFitWithinScreenAdvanceOutputActivityTimestamp() throws Exception {
        TerminalEmulator emulator = newEmulator();
        TerminalSession session = sessionWithEmulator(emulator);

        emitGenuineOutput(emulator, "first reply line\r\n");
        client.onGenuineOutput(session);
        Assert.assertNull("the first observation only establishes the baseline counter", lastOutputActivity());
        Assert.assertEquals("the lines printed so far must fit within the visible screen without scrolling off the top",
            0L, emulator.getNeverResetScrolledLineCount());

        emitGenuineOutput(emulator, "second reply line\r\nthird reply line\r\n");
        client.onGenuineOutput(session);

        Assert.assertEquals("output that fits within the visible screen must not have scrolled off the top",
            0L, emulator.getNeverResetScrolledLineCount());
        Assert.assertNotNull("output that fits within the visible screen must still record output activity",
            lastOutputActivity());
    }

    @Test
    public void newLineScrolledIntoHistoryAdvancesOutputActivityTimestamp() throws Exception {
        TerminalEmulator emulator = newEmulator();
        TerminalSession session = sessionWithEmulator(emulator);

        fillScreen(emulator);
        client.onGenuineOutput(session);

        emitGenuineOutput(emulator, "assistant message line\r\n");
        client.onGenuineOutput(session);

        Assert.assertNotNull(lastOutputActivity());
    }

    @Test
    public void multiRowInPlaceRepaintDoesNotAdvanceOutputActivityTimestamp() throws Exception {
        TerminalEmulator emulator = newEmulator();
        TerminalSession session = sessionWithEmulator(emulator);

        fillScreen(emulator);
        client.onGenuineOutput(session);
        emitGenuineOutput(emulator, "first new line\r\nsecond new line\r\n");
        client.onGenuineOutput(session);
        Long afterRealOutput = lastOutputActivity();
        Assert.assertNotNull(afterRealOutput);

        long scrollbackBeforeRepaint = emulator.getNeverResetScrolledLineCount();
        for (int frame = 0; frame < 30; frame++) {
            emitGenuineOutput(emulator,
                "\033[20;1H* Thinking" + (frame % 2 == 0 ? "..." : ".  ")
                    + "\033[23;1H| > typing" + frame + "         |"
                    + "\033[24;1H| context: " + (1234 + frame) + " tokens |");
            client.onGenuineOutput(session);
        }
        Assert.assertEquals("a multi-row in-place repaint must not grow scrollback history",
            scrollbackBeforeRepaint, emulator.getNeverResetScrolledLineCount());

        Assert.assertEquals(afterRealOutput, lastOutputActivity());
    }

    @Test
    public void scrollAndRedrawThroughOnTextChangedNeverAdvanceOutputActivityTimestamp() throws Exception {
        TerminalEmulator emulator = newEmulator();
        TerminalSession session = sessionWithEmulator(emulator);

        fillScreen(emulator);
        emitGenuineOutput(emulator, "new line one\r\nnew line two\r\n");
        client.onTextChanged(session);
        client.onTextChanged(session);
        client.onTextChanged(session);

        Assert.assertNull(lastOutputActivity());
    }

    @Test
    public void alternateBufferInternalScrollDoesNotAdvanceOutputActivityTimestamp() throws Exception {
        TerminalEmulator emulator = newEmulator();
        TerminalSession session = sessionWithEmulator(emulator);

        fillScreen(emulator);
        client.onGenuineOutput(session);
        emitGenuineOutput(emulator, "history line one\r\nhistory line two\r\n");
        client.onGenuineOutput(session);
        Long afterRealOutput = lastOutputActivity();
        Assert.assertNotNull(afterRealOutput);

        emitGenuineOutput(emulator, "\033[?1049h");
        Assert.assertTrue(emulator.isAlternateBufferActive());
        long scrollbackBeforeAltScroll = emulator.getNeverResetScrolledLineCount();
        for (int line = 0; line < 40; line++) {
            emitGenuineOutput(emulator, "alt screen line " + line + "\r\n");
            client.onGenuineOutput(session);
        }
        Assert.assertEquals("internal scrolling in the alternate screen buffer must not grow scrollback history",
            scrollbackBeforeAltScroll, emulator.getNeverResetScrolledLineCount());

        Assert.assertEquals(afterRealOutput, lastOutputActivity());
    }
}
