package com.termux.app.diagnostics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScrollWithoutDrawEpisodeRecorderTest {

    @Test
    public void anEpisodeIsRecordedWhenTheTerminalWasScrolledLongerAgoThanItLastDrew() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        boolean recorded = recorder.recordEpisode(20_000L, 10_000L);

        assertTrue("the reading that separates a stopped frame pipeline from a working one only"
                + " exists while the episode is happening, and the report can only be produced"
                + " through the interface that has stopped responding, so an episode that is not"
                + " recorded as it happens leaves no trace once the terminal draws again",
            recorded);
    }

    @Test
    public void nothingIsRecordedWhenTheTerminalDrewAfterTheLastScrollStep() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        boolean recorded = recorder.recordEpisode(10_000L, 20_000L);

        assertFalse("a terminal that drew after it was last scrolled is answering scrolling, so"
                + " recording that as an episode would fill the log with the ordinary case and"
                + " leave the real one indistinguishable",
            recorded);
    }

    @Test
    public void nothingIsRecordedWhileTheScrollIsStillWithinTheThreshold() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        boolean recorded = recorder.recordEpisode(
            10_000L + ScrollWithoutDrawEpisodeRecorder.UNDRAWN_AFTER_SCROLL_THRESHOLD_MILLIS - 1L,
            10_000L);

        assertFalse("a terminal that has not yet drawn for the most recent scroll step is mid-frame"
                + " rather than stopped, so the threshold is what keeps ordinary frame latency out"
                + " of the log",
            recorded);
    }

    @Test
    public void aContinuingEpisodeIsRecordedOnceRatherThanOnEveryCycle() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        assertTrue(recorder.recordEpisode(20_000L, 10_000L));
        boolean recordedAgain = recorder.recordEpisode(40_000L, 10_000L);

        assertFalse("the owner keeps scrolling while the terminal stays dead, so recording on every"
                + " cycle would push the rest of the log out of the bounded buffer and destroy the"
                + " evidence this record exists to keep",
            recordedAgain);
    }

    @Test
    public void aNewEpisodeIsRecordedOnceTheTerminalHasDrawnAgain() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        assertTrue(recorder.recordEpisode(20_000L, 10_000L));
        boolean recordedAfterRecovery = recorder.recordEpisode(60_000L, 50_000L);

        assertTrue("a second episode after the terminal recovered is a separate occurrence, and"
                + " suppressing it would report a defect that happens repeatedly as if it had"
                + " happened once",
            recordedAfterRecovery);
    }
}
