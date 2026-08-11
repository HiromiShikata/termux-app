package com.termux.app.browser;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class OpenTagUrlOnTerminalTest {

    private static final int SCREEN_COLUMNS = 80;

    private static final int SCREEN_ROWS = 24;

    private static final String URL_HEAD = "https://example.com/projects/AAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private static final String URL_TAIL = "BBBBBBBBBBBB/";

    private static final String WHOLE_URL = URL_HEAD + URL_TAIL;

    @Test
    public void aLongPressOnEitherRowOfAUrlTheWritingProgramBrokeAcrossRowsResolvesTheWholeUrl() {
        TerminalEmulator emulator = emulatorShowing(
            "<open> " + URL_HEAD + "\r\n  " + URL_TAIL + " </open>\r\n");

        Assert.assertEquals("a long press on the row carrying the head of the URL must resolve the whole "
                + "URL, because the content of an open tag is a pure URL and the row break is only how the "
                + "writing program wrapped its own output",
            WHOLE_URL, OpenTagUrlOnTerminal.urlCoveringRow(emulator, 0));
        Assert.assertEquals("a long press on the row carrying the tail of the URL must resolve the same "
                + "whole URL",
            WHOLE_URL, OpenTagUrlOnTerminal.urlCoveringRow(emulator, 1));
    }

    @Test
    public void aLongPressOnAUrlAnOpenTagCarriesOnASingleRowResolvesThatUrl() {
        String urlOnOneRow = "https://example.com/search?q=hello+world&page=7#section";
        TerminalEmulator emulator = emulatorShowing("<open> " + urlOnOneRow + " </open>\r\n");

        Assert.assertEquals(urlOnOneRow, OpenTagUrlOnTerminal.urlCoveringRow(emulator, 0));
    }

    @Test
    public void aLongPressOnARowNoOpenTagCoversResolvesNothing() {
        TerminalEmulator emulator = emulatorShowing(
            "plain output carrying " + WHOLE_URL + " outside any tag\r\n");

        Assert.assertNull("a row that no open tag covers must be left to the existing word extraction, "
                + "which is what keeps a bare URL and an OSC 8 hyperlink working",
            OpenTagUrlOnTerminal.urlCoveringRow(emulator, 0));
    }

    @Test
    public void aLongPressInsideAnOpenTagLeftUnclosedResolvesNothing() {
        TerminalEmulator emulator = emulatorShowing("<open> " + URL_HEAD + "\r\n");

        Assert.assertNull("a tag still being written names no URL yet",
            OpenTagUrlOnTerminal.urlCoveringRow(emulator, 0));
    }

    private static TerminalEmulator emulatorShowing(String output) {
        TerminalEmulator emulator = new TerminalEmulator(new SilentTerminalOutput(),
            SCREEN_COLUMNS, SCREEN_ROWS, 13, 15, 1000, null);
        byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
        emulator.append(bytes, bytes.length);
        return emulator;
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
