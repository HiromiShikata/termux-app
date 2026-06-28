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
    public void alternateBufferDisallowsInAppScrollSoTheHostTmuxScrollCommandIsUsedInstead() {
        CapturingTerminalOutput output = new CapturingTerminalOutput();
        TerminalEmulator emulator = new TerminalEmulator(output, 80, 24, 13, 15, 1000, null);

        byte[] enterAlternateBuffer = "\033[?1049h".getBytes(StandardCharsets.UTF_8);
        emulator.append(enterAlternateBuffer, enterAlternateBuffer.length);
        Assert.assertTrue("the foreground program is on the alternate screen buffer",
            emulator.isAlternateBufferActive());

        boolean allowsInAppScroll =
            CallToUserScrollDecision.allowsInAppScroll(emulator.isAlternateBufferActive());

        Assert.assertFalse(
            "the in-app setTopRow path must not be used while the alternate buffer is active,"
                + " because the alternate screen buffer has no scrollback",
            allowsInAppScroll);
    }

    @Test
    public void hostTmuxScrollCommandIsNotInjectedAsRawShellTextIntoTheForegroundProgram() {
        String scrollCommand = HostTmuxCallToUserScrollCommand.forSessionName("umino");

        Assert.assertFalse(
            "the scroll command must not contain a bare 'tmux copy-mode' shell line, because that"
                + " text was injected into the foreground program instead of being executed",
            scrollCommand.contains("tmux copy-mode"));
        Assert.assertFalse(
            "the scroll command must not contain a bare 'tmux send-keys' shell line",
            scrollCommand.contains("tmux send-keys"));
    }

    @Test
    public void hostTmuxScrollCommandUsesTheTmuxPrefixKeyCommandPromptSoTmuxInterceptsIt() {
        String scrollCommand = HostTmuxCallToUserScrollCommand.forSessionName("umino");

        Assert.assertEquals(
            "the command must start with the tmux prefix key so tmux captures it regardless of the"
                + " foreground program",
            HostTmuxCallToUserScrollCommand.DEFAULT_TMUX_PREFIX_KEY, scrollCommand.charAt(0));
        Assert.assertTrue(
            "the prefix key must be followed by the tmux command-prompt ':' so the command is run"
                + " by tmux rather than typed into the foreground program",
            scrollCommand.charAt(1) == ':');
    }
}
