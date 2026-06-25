package com.termux.terminal;

import java.nio.charset.StandardCharsets;

public class RealOutputVersionEchoExclusionTest extends TerminalTestCase {

    private final TerminalInputEchoFilter mEchoFilter = new TerminalInputEchoFilter();

    private void recordUserInput(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        mEchoFilter.recordUserInput(bytes, 0, bytes.length);
    }

    private void appendSessionOutputThroughHandlerModel(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int genuineOffset = mEchoFilter.consumeEchoPrefixReturningGenuineOffset(bytes, 0, bytes.length);
        if (genuineOffset > 0) mTerminal.append(bytes, genuineOffset);
        mTerminal.appendGenuineOutput(bytes, genuineOffset, bytes.length - genuineOffset);
        assertInvariants();
    }

    private String idleStatusBarClockRepaint(String clock) {
        return "\033[?25l\033[30m\033[42m\033[24;1H[0] 0:bash*"
            + "                                                        " + clock
            + "\033(B\033[m\033[?12l\033[?25h\033[1;11H";
    }

    public void testKeystrokeEchoInterleavedWithIdleRedrawDoesNotAdvanceCounter() {
        withTerminalSized(80, 24);
        appendSessionOutputThroughHandlerModel(idleStatusBarClockRepaint("12:00:00"));
        long versionBefore = mTerminal.getRealOutputVersion();

        recordUserInput("l");
        appendSessionOutputThroughHandlerModel("l" + idleStatusBarClockRepaint("12:00:01"));
        recordUserInput("s");
        appendSessionOutputThroughHandlerModel("s" + idleStatusBarClockRepaint("12:00:02"));

        assertEquals("keystroke echo on the prompt row plus a single-row status repaint must not advance the real-output version",
            versionBefore, mTerminal.getRealOutputVersion());
    }

    public void testRealOutputAfterEnterAdvancesCounter() {
        withTerminalSized(80, 24);
        recordUserInput("ls\r");
        appendSessionOutputThroughHandlerModel("ls\r\nfile-one.txt\r\nfile-two.txt\r\n");

        assertTrue("genuine command output after the echoed command must advance the real-output version",
            mTerminal.getRealOutputVersion() > 0L);
    }
}
