package com.termux.terminal;

public class TmuxDcsPassthroughTest extends TerminalTestCase {

    public void testTmuxPassthroughUnwrapsInnerWindowTitleOsc() {
        withTerminalSized(4, 2)
            .enterString("\033Ptmux;\033\033]0;passthrough title\007\033\\");

        assertEquals(1, mOutput.titleChanges.size());
        assertEquals("passthrough title", mOutput.titleChanges.get(0).newTitle);
    }

}
