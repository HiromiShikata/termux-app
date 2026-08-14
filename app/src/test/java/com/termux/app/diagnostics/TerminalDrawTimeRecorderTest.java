package com.termux.app.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerminalDrawTimeRecorderTest {

    @Test
    public void aProcessThatHasNotDrawnYetSaysSoRatherThanReportingNoDelay() {
        TerminalDrawTimeRecorder recorder = new TerminalDrawTimeRecorder();

        DiagnosticsTerminalDrawTime drawTime = recorder.snapshot(12_000L);

        assertFalse("a terminal that has never drawn would otherwise report zero milliseconds since its"
            + " last draw, which reads as a frame that has just been produced", drawTime.hasDrawn());
    }

    @Test
    public void theAgeOfTheLastDrawIsMeasuredFromWhenItWasRecorded() {
        TerminalDrawTimeRecorder recorder = new TerminalDrawTimeRecorder();

        recorder.record(10_000L);
        DiagnosticsTerminalDrawTime drawTime = recorder.snapshot(11_843L);

        assertTrue(drawTime.hasDrawn());
        assertEquals("the age of the last frame is what says whether the frame pipeline was still running"
            + " when the reading was taken", 1_843L, drawTime.getMillisSinceLastDraw());
    }

    @Test
    public void aLaterDrawReplacesTheOneBeforeIt() {
        TerminalDrawTimeRecorder recorder = new TerminalDrawTimeRecorder();

        recorder.record(10_000L);
        recorder.record(10_900L);
        DiagnosticsTerminalDrawTime drawTime = recorder.snapshot(11_000L);

        assertEquals("keeping the first draw instead of the most recent one would report a frozen"
            + " pipeline for an app that is drawing normally", 100L, drawTime.getMillisSinceLastDraw());
    }
}
