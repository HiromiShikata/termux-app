package com.termux.terminal;

public class NeverResetScrolledLineCountTest extends TerminalTestCase {

    public void testCounterStartsAtZero() {
        withTerminalSized(5, 3);

        assertEquals(0L, mTerminal.getNeverResetScrolledLineCount());
    }

    public void testGenuineNewOutputScrollingAdvancesCounter() {
        withTerminalSized(5, 3);

        enterString("a\r\nb\r\nc\r\nd\r\ne");

        assertTrue("scrolled-line count should advance once content scrolls past the screen",
            mTerminal.getNeverResetScrolledLineCount() > 0L);
    }

    public void testInPlaceRepaintDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        enterString("a\r\nb\r\nc");
        long countAfterInitialOutput = mTerminal.getNeverResetScrolledLineCount();

        for (int repaint = 0; repaint < 20; repaint++) {
            enterString("\033[1;1H");
            enterString("spinner-" + (repaint % 10));
        }

        assertEquals("cursor-addressed in-place repaints must not advance the scrolled-line count",
            countAfterInitialOutput, mTerminal.getNeverResetScrolledLineCount());
    }

    public void testCounterIsNeverResetByRenderScrollCounterClear() {
        withTerminalSized(5, 3);
        enterString("a\r\nb\r\nc\r\nd\r\ne");
        long countBeforeClear = mTerminal.getNeverResetScrolledLineCount();

        mTerminal.clearScrollCounter();

        assertEquals(countBeforeClear, mTerminal.getNeverResetScrolledLineCount());
        assertTrue(countBeforeClear > 0L);
    }

    public void testAlternateBufferInternalScrollDoesNotAdvanceCounter() {
        withTerminalSized(10, 3);
        enterString("a\r\nb\r\nc");
        enterString("\033[?1049h");
        assertTrue(mTerminal.isAlternateBufferActive());
        long countAfterEnteringAltBuffer = mTerminal.getNeverResetScrolledLineCount();

        enterString("x\r\ny\r\nz\r\nw\r\nv\r\nu");

        assertEquals("internal scrolling in the alternate screen buffer must not advance the scrolled-line count",
            countAfterEnteringAltBuffer, mTerminal.getNeverResetScrolledLineCount());
    }

    public void testAlternateBufferScrollStillAdvancesRenderScrollCounter() {
        withTerminalSized(10, 3);
        enterString("\033[?1049h");
        assertTrue(mTerminal.isAlternateBufferActive());
        mTerminal.clearScrollCounter();

        enterString("x\r\ny\r\nz\r\nw\r\nv\r\nu");

        assertTrue("render-side scroll counter must still track alt-buffer scrolling so text selection follows it",
            mTerminal.getScrollCounter() > 0);
    }
}
