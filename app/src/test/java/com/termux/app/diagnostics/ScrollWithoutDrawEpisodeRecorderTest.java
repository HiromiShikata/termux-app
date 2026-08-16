package com.termux.app.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class ScrollWithoutDrawEpisodeRecorderTest {

    @Test
    public void anEpisodeIsRecordedWhenTheTerminalWasScrolledLongerAgoThanItLastDrew() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        boolean recorded = recorder.recordEpisode(20_000L, 10_000L, 5_000L);

        assertTrue("the reading that separates a stopped frame pipeline from a working one only"
                + " exists while the episode is happening, and the report can only be produced"
                + " through the interface that has stopped responding, so an episode that is not"
                + " recorded as it happens leaves no trace once the terminal draws again",
            recorded);
    }

    @Test
    public void nothingIsRecordedWhenTheTerminalDrewAfterTheLastScrollStep() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        boolean recorded = recorder.recordEpisode(10_000L, 20_000L, 5_000L);

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
            10_000L, 5_000L);

        assertFalse("a terminal that has not yet drawn for the most recent scroll step is mid-frame"
                + " rather than stopped, so the threshold is what keeps ordinary frame latency out"
                + " of the log",
            recorded);
    }

    @Test
    public void aContinuingEpisodeIsRecordedOnceRatherThanOnEveryCycle() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        assertTrue(recorder.recordEpisode(20_000L, 10_000L, 5_000L));
        boolean recordedAgain = recorder.recordEpisode(40_000L, 10_000L, 5_000L);

        assertFalse("the owner keeps scrolling while the terminal stays dead, so recording on every"
                + " cycle would push the rest of the log out of the bounded buffer and destroy the"
                + " evidence this record exists to keep",
            recordedAgain);
    }

    @Test
    public void theSameConditionIsRecordedOnceEvenThoughTheDerivedLastDrawTimeMoves() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        assertTrue(recorder.recordEpisode(20_000L, 10_000L, 5_000L));
        boolean recordedAgain = recorder.recordEpisode(20_000L, 9_996L, 5_000L);

        assertFalse("the caller converts a monotonic draw instant into a wall clock time against a"
                + " reading taken a few milliseconds apart on every cycle, so the same draw arrives"
                + " with a slightly different value each time, and a recorder that treats each of"
                + " those as a separate draw fills every retained slot with one stale condition and"
                + " discards the genuinely distinct episode that follows it",
            recordedAgain);
    }

    @Test
    public void aNewEpisodeIsRecordedOnceTheTerminalHasDrawnAgain() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        assertTrue(recorder.recordEpisode(20_000L, 10_000L, 5_000L));
        boolean recordedAfterRecovery = recorder.recordEpisode(60_000L, 50_000L, 45_000L);

        assertTrue("a second episode after the terminal recovered is a separate occurrence, and"
                + " suppressing it would report a defect that happens repeatedly as if it had"
                + " happened once",
            recordedAfterRecovery);
    }

    @Test
    public void theRecordedEpisodeCarriesHowLongTheTerminalHadNotDrawn() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        recorder.recordEpisode(20_000L, 10_000L, 5_000L);
        List<ScrollWithoutDrawEpisode> episodes = recorder.getEpisodes();

        assertEquals(1, episodes.size());
        assertEquals("a report read after the application recovered can only name the episode if"
                + " the gap between the scroll and the last draw was kept with it",
            10_000L, episodes.get(0).getUndrawnForMillis());
        assertEquals(20_000L, episodes.get(0).getScrolledAtMillis());
    }

    @Test
    public void onlyTheMostRecentEpisodesAreRetained() {
        ScrollWithoutDrawEpisodeRecorder recorder = new ScrollWithoutDrawEpisodeRecorder();

        int recordedEpisodes = ScrollWithoutDrawEpisodeRecorder.MAX_RETAINED_EPISODES + 1;
        long lastScrolledAtMillis = 0L;
        for (int episodeIndex = 0; episodeIndex < recordedEpisodes; episodeIndex++) {
            long drawAtMillis = 10_000L + episodeIndex * 100_000L;
            lastScrolledAtMillis = drawAtMillis + 10_000L;
            recorder.recordEpisode(lastScrolledAtMillis, drawAtMillis,
                5_000L + episodeIndex * 100_000L);
        }
        List<ScrollWithoutDrawEpisode> episodes = recorder.getEpisodes();

        assertEquals("the section that prints these episodes is a fixed part of a report with a"
                + " character ceiling, so an unbounded list would push the evidence around it out",
            ScrollWithoutDrawEpisodeRecorder.MAX_RETAINED_EPISODES, episodes.size());
        assertEquals("dropping the newest instead of the oldest would keep the first episode after"
                + " a restart and lose the one the owner is asking about",
            lastScrolledAtMillis, episodes.get(episodes.size() - 1).getScrolledAtMillis());
    }
}
