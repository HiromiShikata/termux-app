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

    public void testMarkerWithoutReasonFiresWithEmptyReason() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done\007");
        assertEquals(1, mOutput.markerNotifications);
        assertEquals("", mOutput.markerReasons.get(0));
    }

    public void testMarkerWithReasonFiresAndCarriesReason() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done;build finished\007");
        assertEquals(1, mOutput.markerNotifications);
        assertEquals("build finished", mOutput.markerReasons.get(0));
    }

    public void testMarkerReasonMayContainSemicolons() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done;step 1; step 2 done\007");
        assertEquals(1, mOutput.markerNotifications);
        assertEquals("step 1; step 2 done", mOutput.markerReasons.get(0));
    }

    public void testMarkerWithEmptyReasonAfterSeparatorFires() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done;\007");
        assertEquals(1, mOutput.markerNotifications);
        assertEquals("", mOutput.markerReasons.get(0));
    }

    public void testPayloadPrefixWithoutSeparatorDoesNotFire() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done-extra\007")
            .assertLinesAre("    ", "    ");
        assertEquals(0, mOutput.markerNotifications);
    }

}
