package com.termux.app.terminal;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class CallToUserScrollNoPtyWriteTest {

    private static final class CapturingTerminalOutput extends TerminalOutput {
        final StringBuilder written = new StringBuilder();

        @Override
        public void write(byte[] data, int offset, int count) {
            written.append(new String(data, offset, count, StandardCharsets.UTF_8));
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

    @Test
    public void alternateBufferScrollToCallToUserWritesNoTmuxCommandToSession() {
        CapturingTerminalOutput output = new CapturingTerminalOutput();
        TerminalEmulator emulator = new TerminalEmulator(output, 80, 24, 13, 15, 1000, null);

        byte[] enterAlternateBuffer = "\033[?1049h".getBytes(StandardCharsets.UTF_8);
        emulator.append(enterAlternateBuffer, enterAlternateBuffer.length);
        Assert.assertTrue("the foreground program is on the alternate screen buffer",
            emulator.isAlternateBufferActive());

        output.written.setLength(0);

        boolean allowsInAppScroll =
            CallToUserScrollDecision.allowsInAppScroll(emulator.isAlternateBufferActive());

        Assert.assertFalse(
            "the scroll-to-call-to-user action must not perform an in-app scroll while the alternate"
                + " buffer is active",
            allowsInAppScroll);
        Assert.assertEquals(
            "the scroll-to-call-to-user action must write nothing to the session PTY while the"
                + " alternate buffer is active, so no tmux command text is injected into the"
                + " foreground program",
            "",
            output.written.toString());
        Assert.assertFalse(
            "no tmux copy-mode command may reach the session PTY from the scroll-to action",
            output.written.toString().contains("tmux copy-mode"));
        Assert.assertFalse(
            "no tmux send-keys command may reach the session PTY from the scroll-to action",
            output.written.toString().contains("tmux send-keys"));
    }
}
