package com.termux.app.terminal;

import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class SessionStatuslineScanTextReuseTest {

    private static final String STATUSLINE_THAT_SCROLLED_OFF =
        "call:the-owner-was-paged-before-the-full-screen-program-started";
    private static final String ENTER_ALTERNATE_BUFFER = "\033[?1049h";
    private static final int SCREEN_ROWS = 24;

    @Test
    public void reusingTheActiveTranscriptYieldsTheSameTextWhileTheMainBufferIsActive() {
        TerminalEmulator emulator = emulatorWithAStatuslineScrolledOffTheScreen();
        TerminalBuffer screen = emulator.getScreen();

        Assert.assertEquals("while the main buffer is the active one the caller's own transcript read is "
                + "the very text the statusline scan would have read for itself, so reusing it must not "
                + "change a single character of the scanned text",
            SessionStatuslineScanText.of(emulator, screen),
            SessionStatuslineScanText.reusingActiveBufferTranscript(
                emulator, screen, screen.getTranscriptText()));
    }

    @Test
    public void reusingTheActiveTranscriptStillReadsTheMainBufferWhileTheAlternateBufferIsActive() {
        TerminalEmulator emulator = emulatorWithAStatuslineScrolledOffTheScreen();
        append(emulator, ENTER_ALTERNATE_BUFFER + "a full-screen program is drawing here\r\n");
        TerminalBuffer alternateScreen = emulator.getScreen();

        Assert.assertTrue("this test is only meaningful while the alternate buffer is the active one",
            emulator.isAlternateBufferActive());
        Assert.assertFalse("the alternate buffer keeps no scrollback, so the statusline that scrolled off "
                + "before the full-screen program started is absent from the active buffer's transcript "
                + "and reusing that transcript unchanged would hide it from the scan",
            alternateScreen.getTranscriptText().contains(STATUSLINE_THAT_SCROLLED_OFF));

        String scanText = SessionStatuslineScanText.reusingActiveBufferTranscript(
            emulator, alternateScreen, alternateScreen.getTranscriptText());

        Assert.assertTrue("a session whose owner call scrolled off the screen before a full-screen program "
                + "took the alternate buffer must still have that call found by the statusline scan, so the "
                + "reuse must fall back to the main-buffer transcript while the alternate buffer is active",
            scanText.contains(STATUSLINE_THAT_SCROLLED_OFF));
        Assert.assertEquals("reusing the caller's transcript must yield exactly the text the statusline "
                + "scan reads for itself, in the alternate-buffer case as well as the main-buffer one",
            SessionStatuslineScanText.of(emulator, alternateScreen), scanText);
    }

    private static TerminalEmulator emulatorWithAStatuslineScrolledOffTheScreen() {
        TerminalEmulator emulator = new TerminalEmulator(new SilentTerminalOutput(),
            80, SCREEN_ROWS, 13, 15, 1000, null);
        append(emulator, STATUSLINE_THAT_SCROLLED_OFF + "\r\n");
        for (int row = 0; row < SCREEN_ROWS * 2; row++) {
            append(emulator, "shell output line " + row + "\r\n");
        }
        Assert.assertFalse("the statusline must have scrolled off the visible screen for this scenario",
            SessionStatuslineScanText.visibleScreenOf(emulator, emulator.getScreen())
                .contains(STATUSLINE_THAT_SCROLLED_OFF));
        return emulator;
    }

    private static void append(TerminalEmulator emulator, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        emulator.append(bytes, bytes.length);
    }

    private static final class SilentTerminalOutput extends TerminalOutput {
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
}
