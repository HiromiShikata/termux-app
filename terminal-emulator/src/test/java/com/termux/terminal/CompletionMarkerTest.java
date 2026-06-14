package com.termux.terminal;

public class CompletionMarkerTest extends TerminalTestCase {

    public void testBelTerminatedMarkerFiresAndPrintsNothing() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done\007")
            .assertLinesAre("    ", "    ");
        assertEquals(1, mOutput.markerNotifications);
    }

    public void testStTerminatedMarkerFiresAndPrintsNothing() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done\033\\")
            .assertLinesAre("    ", "    ");
        assertEquals(1, mOutput.markerNotifications);
    }

    public void testNonMatchingPayloadDoesNotFire() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;something-else\007")
            .assertLinesAre("    ", "    ");
        assertEquals(0, mOutput.markerNotifications);
    }

    public void testDifferentOscCodeDoesNotFire() {
        withTerminalSized(4, 2)
            .enterString("\033]9998;claude-done\007")
            .assertLinesAre("    ", "    ");
        assertEquals(0, mOutput.markerNotifications);
    }

}
