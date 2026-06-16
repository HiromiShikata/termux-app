package com.termux.terminal;

public class UrgentMarkerTest extends TerminalTestCase {

    public void testBelTerminatedUrgentMarkerFiresAndPrintsNothing() {
        withTerminalSized(4, 2)
            .enterString("\033]9998;claude-urgent\007")
            .assertLinesAre("    ", "    ");
        assertEquals(1, mOutput.urgentNotifications);
        assertEquals(0, mOutput.markerNotifications);
    }

    public void testStTerminatedUrgentMarkerFiresAndPrintsNothing() {
        withTerminalSized(4, 2)
            .enterString("\033]9998;claude-urgent\033\\")
            .assertLinesAre("    ", "    ");
        assertEquals(1, mOutput.urgentNotifications);
        assertEquals(0, mOutput.markerNotifications);
    }

    public void testNonMatchingUrgentPayloadDoesNotFire() {
        withTerminalSized(4, 2)
            .enterString("\033]9998;something-else\007")
            .assertLinesAre("    ", "    ");
        assertEquals(0, mOutput.urgentNotifications);
    }

    public void testCompletionOscCodeWithUrgentPayloadDoesNotFireUrgent() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-urgent\007")
            .assertLinesAre("    ", "    ");
        assertEquals(0, mOutput.urgentNotifications);
        assertEquals(0, mOutput.markerNotifications);
    }

    public void testCompletionMarkerFiresMarkerAndNotUrgent() {
        withTerminalSized(4, 2)
            .enterString("\033]9999;claude-done\007")
            .assertLinesAre("    ", "    ");
        assertEquals(1, mOutput.markerNotifications);
        assertEquals(0, mOutput.urgentNotifications);
    }

    public void testUrgentOscCodeWithCompletionPayloadDoesNotFire() {
        withTerminalSized(4, 2)
            .enterString("\033]9998;claude-done\007")
            .assertLinesAre("    ", "    ");
        assertEquals(0, mOutput.urgentNotifications);
        assertEquals(0, mOutput.markerNotifications);
    }

}
