package com.termux.terminal;

public class Osc8HyperlinkTest extends TerminalTestCase {

    public void testHyperlinkUriAttachedToVisibleCells() {
        withTerminalSized(20, 2)
            .enterString("\033]8;;https://example.com\033\\Example Site\033]8;;\033\\")
            .assertLineIs(0, "Example Site        ");
        TerminalBuffer screen = mTerminal.getScreen();
        for (int column = 0; column < "Example Site".length(); column++)
            assertEquals("column=" + column, "https://example.com", screen.getHyperlinkUri(0, column));
    }

    public void testCellsAfterCloseHaveNoHyperlink() {
        withTerminalSized(20, 2)
            .enterString("\033]8;;https://example.com\033\\Link\033]8;;\033\\ plain");
        TerminalBuffer screen = mTerminal.getScreen();
        assertEquals("https://example.com", screen.getHyperlinkUri(0, 0));
        for (int column = 4; column < 10; column++)
            assertNull("column=" + column, screen.getHyperlinkUri(0, column));
    }

    public void testCellsBeforeHyperlinkHaveNoHyperlink() {
        withTerminalSized(20, 2)
            .enterString("plain \033]8;;https://example.com\033\\Link\033]8;;\033\\");
        TerminalBuffer screen = mTerminal.getScreen();
        for (int column = 0; column < 6; column++)
            assertNull("column=" + column, screen.getHyperlinkUri(0, column));
        assertEquals("https://example.com", screen.getHyperlinkUri(0, 6));
    }

    public void testBelTerminatorIsSupported() {
        withTerminalSized(20, 2)
            .enterString("\033]8;;https://example.com\007Link\033]8;;\007");
        assertEquals("https://example.com", mTerminal.getScreen().getHyperlinkUri(0, 0));
    }

    public void testParamsBeforeUriAreIgnored() {
        withTerminalSized(20, 2)
            .enterString("\033]8;id=anchor1;https://example.com/page\033\\Doc\033]8;;\033\\");
        assertEquals("https://example.com/page", mTerminal.getScreen().getHyperlinkUri(0, 0));
    }

    public void testSwitchingUriWithoutExplicitClose() {
        withTerminalSized(20, 2)
            .enterString("\033]8;;https://a.example\033\\AA\033]8;;https://b.example\033\\BB\033]8;;\033\\");
        TerminalBuffer screen = mTerminal.getScreen();
        assertEquals("https://a.example", screen.getHyperlinkUri(0, 0));
        assertEquals("https://a.example", screen.getHyperlinkUri(0, 1));
        assertEquals("https://b.example", screen.getHyperlinkUri(0, 2));
        assertEquals("https://b.example", screen.getHyperlinkUri(0, 3));
    }

    public void testHyperlinkPreservedAfterScrollIntoTranscript() {
        withTerminalSized(20, 2)
            .enterString("\033]8;;https://example.com\033\\Link\033]8;;\033\\\r\nrow2\r\nrow3");
        // "Link" was on the first screen row; after two newlines it scrolled one row up into the transcript (-1).
        assertEquals("https://example.com", mTerminal.getScreen().getHyperlinkUri(-1, 0));
    }

    public void testOverwritingHyperlinkCellClearsUri() {
        withTerminalSized(20, 2)
            .enterString("\033]8;;https://example.com\033\\Link\033]8;;\033\\")
            .enterString("\r")
            .enterString("X");
        TerminalBuffer screen = mTerminal.getScreen();
        assertNull(screen.getHyperlinkUri(0, 0));
        assertEquals("https://example.com", screen.getHyperlinkUri(0, 1));
    }

    public void testResetClearsActiveHyperlink() {
        withTerminalSized(20, 2)
            .enterString("\033]8;;https://example.com\033\\Link")
            .enterString("\033c")
            .enterString("plain");
        assertNull(mTerminal.getScreen().getHyperlinkUri(0, 0));
    }

    public void testOutOfBoundsLookupReturnsNull() {
        withTerminalSized(20, 2).enterString("hi");
        TerminalBuffer screen = mTerminal.getScreen();
        assertNull(screen.getHyperlinkUri(0, -1));
        assertNull(screen.getHyperlinkUri(0, 20));
        assertNull(screen.getHyperlinkUri(99, 0));
    }

}
