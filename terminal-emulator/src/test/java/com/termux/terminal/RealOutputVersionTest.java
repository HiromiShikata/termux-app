package com.termux.terminal;

import java.nio.charset.StandardCharsets;

public class RealOutputVersionTest extends TerminalTestCase {

    private static final int STATUS_ROW = 24;

    private void appendGenuine(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        mTerminal.appendGenuineOutput(bytes, 0, bytes.length);
        assertInvariants();
    }

    private String idleStatusBarClockRepaint(String clock) {
        return "\033[?25l\033[30m\033[42m\033[" + STATUS_ROW + ";1H[0] 0:bash*"
            + "                                                        " + clock
            + "\033(B\033[m\033[?12l\033[?25h\033[1;11H";
    }

    public void testCounterStartsAtZero() {
        withTerminalSized(80, 24);

        assertEquals(0L, mTerminal.getRealOutputVersion());
    }

    public void testIdleStatusBarClockRepaintDoesNotAdvanceCounter() {
        withTerminalSized(80, 24);
        appendGenuine(idleStatusBarClockRepaint("12:00:00"));
        long versionAfterFirstPaint = mTerminal.getRealOutputVersion();

        appendGenuine(idleStatusBarClockRepaint("12:00:01"));
        appendGenuine(idleStatusBarClockRepaint("12:00:02"));
        appendGenuine(idleStatusBarClockRepaint("12:00:03"));

        assertEquals("a tmux status-bar clock that repaints only its own row must not advance the real-output version",
            versionAfterFirstPaint, mTerminal.getRealOutputVersion());
    }

    public void testRealMultiRowOutputAdvancesCounter() {
        withTerminalSized(80, 24);
        appendGenuine(idleStatusBarClockRepaint("12:00:00"));
        long versionBeforeRealOutput = mTerminal.getRealOutputVersion();

        appendGenuine("first output line\r\nsecond output line\r\n");

        assertTrue("real program output spanning multiple rows must advance the real-output version",
            mTerminal.getRealOutputVersion() > versionBeforeRealOutput);
    }

    public void testRealOutputThenIdleKeepsCounterStable() {
        withTerminalSized(80, 24);
        appendGenuine("command output line one\r\ncommand output line two\r\n");
        long versionAfterRealOutput = mTerminal.getRealOutputVersion();

        for (int second = 0; second < 5; second++) {
            appendGenuine(idleStatusBarClockRepaint("12:01:0" + second));
        }

        assertEquals("after real output, periodic idle status-bar repaints must leave the real-output version unchanged so out: keeps counting up",
            versionAfterRealOutput, mTerminal.getRealOutputVersion());
    }

    public void testCursorAddressedSingleRowRepaintDoesNotAdvanceCounter() {
        withTerminalSized(80, 24);
        appendGenuine("\033[?1049h\033[2J\033[1;1Hcontent");
        long versionAfterPaint = mTerminal.getRealOutputVersion();

        for (int frame = 0; frame < 10; frame++) {
            appendGenuine("\033[" + STATUS_ROW + ";1Hspinner-" + (frame % 10));
        }

        assertEquals("a single-row in-place repaint must not advance the real-output version",
            versionAfterPaint, mTerminal.getRealOutputVersion());
    }
}
