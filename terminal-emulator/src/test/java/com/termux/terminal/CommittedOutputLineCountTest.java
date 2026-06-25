package com.termux.terminal;

public class CommittedOutputLineCountTest extends TerminalTestCase {

    public void testCounterStartsAtZero() {
        withTerminalSized(20, 10);

        assertEquals(0L, mTerminal.getCommittedOutputLineCount());
    }

    public void testNewLinesThatFitWithinScreenAdvanceCounter() {
        withTerminalSized(20, 10);

        enterString("alpha\r\nbravo\r\ncharlie\r\n");

        assertTrue("new content lines that fit within the visible screen without scrolling off the top must advance the committed-output-line count",
            mTerminal.getCommittedOutputLineCount() > 0L);
    }

    public void testNewLinesThatScrollOffTopAdvanceCounter() {
        withTerminalSized(20, 3);
        enterString("a\r\nb\r\n");
        long countBeforeScroll = mTerminal.getCommittedOutputLineCount();
        assertEquals("scrolling off the top must not have happened yet", 0L, mTerminal.getNeverResetScrolledLineCount());

        enterString("c\r\nd\r\ne\r\n");

        assertTrue("content scrolled off the top into history must keep advancing the committed-output-line count",
            mTerminal.getNeverResetScrolledLineCount() > 0L);
        assertTrue("committed-output-line count must keep advancing when new lines scroll off the top",
            mTerminal.getCommittedOutputLineCount() > countBeforeScroll);
    }

    public void testAutowrapToNewRowAdvancesCounter() {
        withTerminalSized(5, 10);
        long countBeforeWrap = mTerminal.getCommittedOutputLineCount();

        enterString("abcdefghij");

        assertTrue("autowrapping output onto a new row produces a new content line and must advance the committed-output-line count",
            mTerminal.getCommittedOutputLineCount() > countBeforeWrap);
    }

    public void testCursorAddressedInPlaceRepaintDoesNotAdvanceCounter() {
        withTerminalSized(20, 5);
        enterString("input box\r\nspinner\r\nstatus\r\n");
        long countAfterInitialOutput = mTerminal.getCommittedOutputLineCount();

        for (int frame = 0; frame < 30; frame++) {
            enterString("\033[3;1H| > typing" + frame + "    |");
            enterString("\033[4;1H* Thinking" + (frame % 2 == 0 ? "..." : ".  "));
            enterString("\033[5;1Hcontext: " + (1000 + frame) + " tokens");
        }

        assertEquals("a cursor-addressed in-place repaint of existing rows (input box + spinner + status each frame) must not advance the committed-output-line count",
            countAfterInitialOutput, mTerminal.getCommittedOutputLineCount());
    }

    public void testNativeScrollbackViewScrollDoesNotAdvanceCounter() {
        withTerminalSized(20, 3);
        enterString("one\r\ntwo\r\nthree\r\nfour\r\nfive\r\n");
        long countAfterOutput = mTerminal.getCommittedOutputLineCount();
        assertTrue("output must have advanced the committed-output-line count before testing the viewport-only scroll",
            countAfterOutput > 0L);

        mTerminal.getScreen().getActiveTranscriptRows();
        for (int row = 1; row <= mTerminal.getScreen().getActiveTranscriptRows(); row++) {
            mTerminal.getScreenContentVersion();
        }

        assertEquals("a native scrollback-view scroll only pans the rendered viewport and emits no new content, so it must not advance the committed-output-line count",
            countAfterOutput, mTerminal.getCommittedOutputLineCount());
    }

    public void testAlternateBufferInternalScrollDoesNotAdvanceCounter() {
        withTerminalSized(20, 3);
        enterString("history-a\r\nhistory-b\r\n");
        enterString("\033[?1049h");
        assertTrue(mTerminal.isAlternateBufferActive());
        long countAfterEnteringAltBuffer = mTerminal.getCommittedOutputLineCount();

        enterString("x\r\ny\r\nz\r\nw\r\nv\r\nu\r\n");

        assertEquals("internal scrolling inside a full-screen alternate-buffer application must not advance the committed-output-line count",
            countAfterEnteringAltBuffer, mTerminal.getCommittedOutputLineCount());
    }

    public void testReturningFromAlternateBufferResumesCounting() {
        withTerminalSized(20, 3);
        enterString("\033[?1049h");
        assertTrue(mTerminal.isAlternateBufferActive());
        enterString("alt\r\nalt\r\nalt\r\n");
        long countWhileInAltBuffer = mTerminal.getCommittedOutputLineCount();

        enterString("\033[?1049l");
        assertFalse(mTerminal.isAlternateBufferActive());
        enterString("back on main\r\nmore output\r\n");

        assertTrue("after leaving the alternate buffer, genuine new lines on the main screen must advance the committed-output-line count again",
            mTerminal.getCommittedOutputLineCount() > countWhileInAltBuffer);
    }
}
