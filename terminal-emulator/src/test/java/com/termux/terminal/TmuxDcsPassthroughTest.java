package com.termux.terminal;

public class TmuxDcsPassthroughTest extends TerminalTestCase {

    public void testTmuxPassthroughOsc9998FiresUrgentNotification() {
        withTerminalSized(4, 2)
            .enterString("\033Ptmux;\033\033]9998;claude-urgent;reason\007\033\\");
        assertEquals(1, mOutput.urgentNotifications);
        assertEquals(0, mOutput.markerNotifications);
    }

    public void testTmuxPassthroughOsc9999FiresMarkerNotification() {
        withTerminalSized(4, 2)
            .enterString("\033Ptmux;\033\033]9999;claude-done;reason\007\033\\");
        assertEquals(1, mOutput.markerNotifications);
        assertEquals(0, mOutput.urgentNotifications);
    }

}
