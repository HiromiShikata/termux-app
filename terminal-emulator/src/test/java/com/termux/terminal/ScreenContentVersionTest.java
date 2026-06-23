package com.termux.terminal;

public class ScreenContentVersionTest extends TerminalTestCase {

    public void testCounterAdvancesWhenGlyphsAreEmitted() {
        withTerminalSized(10, 3);
        long versionBefore = mTerminal.getScreenContentVersion();

        enterString("hello");

        assertTrue("printing visible characters must advance the screen-content version",
            mTerminal.getScreenContentVersion() > versionBefore);
    }

    public void testInPlaceCellOverwriteAdvancesCounter() {
        withTerminalSized(10, 3);
        enterString("hello");
        long versionAfterFirstPaint = mTerminal.getScreenContentVersion();

        enterString("\033[1;1Hworld");

        assertTrue("overwriting existing cells in place without scrolling must advance the screen-content version",
            mTerminal.getScreenContentVersion() > versionAfterFirstPaint);
    }

    public void testRepaintingSameContentInPlaceAdvancesCounter() {
        withTerminalSized(10, 3);
        enterString("\033[1;1Hstatus");
        long versionAfterFirstPaint = mTerminal.getScreenContentVersion();

        enterString("\033[1;1Hstatus");

        assertTrue("a full-screen UI redraw that rewrites the same cells must advance the screen-content version",
            mTerminal.getScreenContentVersion() > versionAfterFirstPaint);
    }

    public void testClearScreenAdvancesCounter() {
        withTerminalSized(10, 3);
        enterString("hello");
        long versionAfterText = mTerminal.getScreenContentVersion();

        enterString("\033[2J");

        assertTrue("clearing the screen rewrites cells and must advance the screen-content version",
            mTerminal.getScreenContentVersion() > versionAfterText);
    }

    public void testCursorMovementEscapeSequenceDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBefore = mTerminal.getScreenContentVersion();

        enterString("\033[2;3H");

        assertEquals("a cursor-position escape sequence writes no cell and must not advance the counter",
            versionBefore, mTerminal.getScreenContentVersion());
    }

    public void testColorChangeEscapeSequenceDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBefore = mTerminal.getScreenContentVersion();

        enterString("\033[31m");

        assertEquals("a color-change escape sequence writes no cell and must not advance the counter",
            versionBefore, mTerminal.getScreenContentVersion());
    }

    public void testWindowTitleOscDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBefore = mTerminal.getScreenContentVersion();

        enterString("\033]0;my title\007");

        assertEquals("an OSC window-title sequence writes no cell and must not advance the counter",
            versionBefore, mTerminal.getScreenContentVersion());
    }
}
