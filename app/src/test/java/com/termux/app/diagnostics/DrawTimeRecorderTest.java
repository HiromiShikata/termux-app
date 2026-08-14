package com.termux.app.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DrawTimeRecorderTest {

    @Test
    public void aProcessThatHasNotDrawnYetSaysSoRatherThanReportingNoDelay() {
        DrawTimeRecorder recorder = new DrawTimeRecorder();
        Object window = new Object();

        DiagnosticsDrawTime drawTime = recorder.snapshot(window, 12_000L);

        assertFalse("a terminal that has never drawn would otherwise report zero milliseconds since its"
            + " last draw, which reads as a frame that has just been produced", drawTime.hasDrawn());
    }

    @Test
    public void theAgeOfTheLastDrawIsMeasuredFromWhenItWasRecorded() {
        DrawTimeRecorder recorder = new DrawTimeRecorder();
        Object window = new Object();

        recorder.record(window, 10_000L);
        DiagnosticsDrawTime drawTime = recorder.snapshot(window, 11_843L);

        assertTrue(drawTime.hasDrawn());
        assertEquals("the age of the last frame is what says whether the frame pipeline was still running"
            + " when the reading was taken", 1_843L, drawTime.getMillisSinceLastDraw());
    }

    @Test
    public void aLaterDrawReplacesTheOneBeforeIt() {
        DrawTimeRecorder recorder = new DrawTimeRecorder();
        Object window = new Object();

        recorder.record(window, 10_000L);
        recorder.record(window, 10_900L);
        DiagnosticsDrawTime drawTime = recorder.snapshot(window, 11_000L);

        assertEquals("keeping the first draw instead of the most recent one would report a frozen"
            + " pipeline for an app that is drawing normally", 100L, drawTime.getMillisSinceLastDraw());
    }

    @Test
    public void aDrawByOneWindowIsNotReportedAsADrawByTheWindowTheReadingDescribes() {
        DrawTimeRecorder recorder = new DrawTimeRecorder();
        Object windowWhoseTeardownDidNotRun = new Object();
        Object windowTheReadingDescribes = new Object();

        recorder.record(windowWhoseTeardownDidNotRun, 10_000L);
        DiagnosticsDrawTime drawTime = recorder.snapshot(windowTheReadingDescribes, 10_040L);

        assertFalse("a window the process is still holding after its teardown did not run keeps its"
            + " draw listener registered, so counting its frames as the frames of the window the"
            + " reading names says the pipeline is running while that window has produced nothing",
            drawTime.hasDrawn());
    }

    @Test
    public void eachWindowKeepsItsOwnLastDrawWhileMoreThanOneIsDrawing() {
        DrawTimeRecorder recorder = new DrawTimeRecorder();
        Object windowWhoseTeardownDidNotRun = new Object();
        Object windowTheReadingDescribes = new Object();

        recorder.record(windowWhoseTeardownDidNotRun, 10_000L);
        recorder.record(windowTheReadingDescribes, 10_500L);
        recorder.record(windowWhoseTeardownDidNotRun, 11_000L);

        assertEquals("keeping only the most recent draw of any window would report the window the"
                + " reading names as having never drawn whenever another window drew after it",
            700L, recorder.snapshot(windowTheReadingDescribes, 11_200L).getMillisSinceLastDraw());
        assertEquals(200L,
            recorder.snapshot(windowWhoseTeardownDidNotRun, 11_200L).getMillisSinceLastDraw());
    }
}
