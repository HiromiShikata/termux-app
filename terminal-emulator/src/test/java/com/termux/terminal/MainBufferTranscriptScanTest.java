package com.termux.terminal;

public class MainBufferTranscriptScanTest extends TerminalTestCase {

    public void testStatuslineScrolledOutOfVisibleScreenIsStillInMainBufferTranscript() {
        withTerminalSized(40, 3);
        enterString("claude  out:12:00:00  reply:12:00:00\r\n");
        enterString("line one\r\n");
        enterString("line two\r\n");
        enterString("line three");

        assertInvariants();

        StringBuilder visibleScreen = new StringBuilder();
        for (int row = 0; row < mTerminal.mRows; row++) {
            visibleScreen.append(mTerminal.getScreen()
                .getSelectedText(0, row, mTerminal.mColumns, row, false, false));
            visibleScreen.append('\n');
        }
        assertTrue("statusline must have scrolled out of the visible screen for this test, "
            + "got visible screen: " + visibleScreen,
            !visibleScreen.toString().contains("reply:12:00:00"));

        String mainTranscript = mTerminal.getMainBufferTranscriptText();
        assertTrue("the scrolled-out statusline must remain in the main buffer transcript, got: "
            + mainTranscript, mainTranscript.contains("reply:12:00:00"));
    }

    public void testStatuslineRemainsInMainBufferTranscriptWhenAlternateBufferActive() {
        withTerminalSized(40, 4);
        enterString("claude  call:12:00:05  reply:12:00:00\r\n");
        enterString("\033[?1049h");
        enterString("full screen TUI content");

        assertTrue("alternate buffer must be active for this test",
            mTerminal.isAlternateBufferActive());

        String alternateScreen = mTerminal.getScreen().getTranscriptText();
        assertTrue("the Claude statusline must not be on the alternate screen, got: "
            + alternateScreen, !alternateScreen.contains("call:12:00:05"));

        String mainTranscript = mTerminal.getMainBufferTranscriptText();
        assertTrue("the statusline must remain in the main buffer transcript while the alternate "
            + "buffer is active, got: " + mainTranscript, mainTranscript.contains("call:12:00:05"));
        assertTrue(mainTranscript.contains("reply:12:00:00"));
    }
}
