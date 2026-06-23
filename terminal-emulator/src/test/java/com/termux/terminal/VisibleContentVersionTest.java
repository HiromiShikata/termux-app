package com.termux.terminal;

public class VisibleContentVersionTest extends TerminalTestCase {

    public void testCounterStartsAtZero() {
        withTerminalSized(10, 3);

        assertEquals(0L, mTerminal.getVisibleContentVersion());
    }

    public void testGenuineVisibleTextAdvancesCounter() {
        withTerminalSized(10, 3);

        enterString("hello");

        assertTrue("printing visible characters must advance the visible-content version",
            mTerminal.getVisibleContentVersion() > 0L);
    }

    public void testUrgentOscMarkerDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBeforeMarker = mTerminal.getVisibleContentVersion();

        enterString("\033]9998;claude-urgent\007");

        assertEquals("an OSC 9998 urgent marker carries no visible output",
            versionBeforeMarker, mTerminal.getVisibleContentVersion());
        assertEquals(1, mOutput.urgentNotifications);
    }

    public void testCompletionOscMarkerDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBeforeMarker = mTerminal.getVisibleContentVersion();

        enterString("\033]9999;claude-done\007");

        assertEquals("an OSC 9999 completion marker carries no visible output",
            versionBeforeMarker, mTerminal.getVisibleContentVersion());
        assertEquals(1, mOutput.markerNotifications);
    }

    public void testCursorMovementEscapeSequenceDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBeforeEscape = mTerminal.getVisibleContentVersion();

        enterString("\033[2;3H");

        assertEquals("a cursor-position escape sequence carries no visible output",
            versionBeforeEscape, mTerminal.getVisibleContentVersion());
    }

    public void testColorChangeEscapeSequenceDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBeforeEscape = mTerminal.getVisibleContentVersion();

        enterString("\033[31m");

        assertEquals("a color-change escape sequence carries no visible output",
            versionBeforeEscape, mTerminal.getVisibleContentVersion());
    }

    public void testWindowTitleOscDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        long versionBeforeOsc = mTerminal.getVisibleContentVersion();

        enterString("\033]0;my title\007");

        assertEquals("an OSC window-title sequence carries no visible output",
            versionBeforeOsc, mTerminal.getVisibleContentVersion());
    }

    public void testGenuineTextAfterControlSequencesStillAdvancesCounter() {
        withTerminalSized(10, 3);
        enterString("\033]9998;claude-urgent\007\033[2;1H\033[31m");
        long versionAfterControlOnly = mTerminal.getVisibleContentVersion();

        enterString("X");

        assertTrue("genuine visible output following control sequences must advance the counter",
            mTerminal.getVisibleContentVersion() > versionAfterControlOnly);
    }
}
