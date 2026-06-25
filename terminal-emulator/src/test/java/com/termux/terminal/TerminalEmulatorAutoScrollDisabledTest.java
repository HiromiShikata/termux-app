package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TerminalEmulatorAutoScrollDisabledTest {

    private TerminalEmulator newEmulator() {
        return new TerminalEmulator(new NoopTerminalOutput(), 30, 5, 13, 15, 2000, null);
    }

    @Test
    public void autoScrollIsEnabledByDefault() {
        Assert.assertFalse(newEmulator().isAutoScrollDisabled());
    }

    @Test
    public void setAutoScrollDisabledTrueHoldsScrollPositionAgainstNewOutput() {
        TerminalEmulator emulator = newEmulator();
        emulator.setAutoScrollDisabled(true);
        Assert.assertTrue(emulator.isAutoScrollDisabled());
    }

    @Test
    public void setAutoScrollDisabledFalseReEnablesAutoScroll() {
        TerminalEmulator emulator = newEmulator();
        emulator.setAutoScrollDisabled(true);
        emulator.setAutoScrollDisabled(false);
        Assert.assertFalse(emulator.isAutoScrollDisabled());
    }

    private static final class NoopTerminalOutput extends TerminalOutput {
        @Override public void write(byte[] data, int offset, int count) {}
        @Override public void titleChanged(String oldTitle, String newTitle) {}
        @Override public void onCopyTextToClipboard(String text) {}
        @Override public void onPasteTextFromClipboard() {}
        @Override public void onBell() {}
        @Override public void onSpeakNotification(String text) {}
        @Override public void onColorsChanged() {}
    }
}
