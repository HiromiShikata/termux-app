package com.termux.terminal;

public class InvisibleAttributePreservesTagTextTest extends TerminalTestCase {

    public void testInvisibleSgrSetsInvisibleAttribute() {
        withTerminalSized(20, 3).enterString("\033[8m");
        assertEquals(TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE, mTerminal.mEffect);
    }

    public void testResetSgrClearsInvisibleAttribute() {
        withTerminalSized(20, 3).enterString("\033[8m\033[0m");
        assertEquals(0, mTerminal.mEffect & TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE);
    }

    public void testConcealOffSgrClearsInvisibleAttribute() {
        withTerminalSized(20, 3).enterString("\033[8m\033[28m");
        assertEquals(0, mTerminal.mEffect & TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE);
    }

    public void testTextEnteredWithInvisibleAttributeRemainsInTranscript() {
        withTerminalSized(40, 3).enterString("\033[8mhidden\033[0m");
        assertTrue(mTerminal.getScreen().getTranscriptText().contains("hidden"));
    }
}
