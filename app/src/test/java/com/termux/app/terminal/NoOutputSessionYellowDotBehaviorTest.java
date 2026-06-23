package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class NoOutputSessionYellowDotBehaviorTest {

    private static final String SESSION = "no-output-session";

    @Test
    public void controlOnlyOutputThatDoesNotEmitVisibleContentIsNotTreatedAsNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();

        long visibleContentVersionAfterPrompt = 5L;
        Assert.assertTrue(tracker.hasNewOutput(SESSION, visibleContentVersionAfterPrompt));

        long unchangedVisibleContentVersion = visibleContentVersionAfterPrompt;
        for (int repaint = 0; repaint < 100; repaint++) {
            Assert.assertFalse(tracker.hasNewOutput(SESSION, unchangedVisibleContentVersion));
        }
    }

    @Test
    public void genuineVisibleOutputAfterControlOnlyOutputIsTreatedAsNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        tracker.hasNewOutput(SESSION, 5L);

        Assert.assertFalse(tracker.hasNewOutput(SESSION, 5L));
        Assert.assertTrue(tracker.hasNewOutput(SESSION, 6L));
    }

    @Test
    public void sessionThatNeverEmittedVisibleContentIsNotTreatedAsNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();

        Assert.assertFalse(tracker.hasNewOutput(SESSION, 0L));
        Assert.assertFalse(tracker.hasNewOutput(SESSION, 0L));
    }

    @Test
    public void noOutputDataOutAgeRendersAsVeryOldRatherThanZeroSecondsAgo() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long realisticNowMillis = 1_750_000_000_000L;
        String outAgeLabel =
            store.lastOutputActivityAgeLabelTreatingNoDataAsVeryOld(SESSION, realisticNowMillis);

        Assert.assertNotEquals("0s ago", outAgeLabel);
        Assert.assertTrue(outAgeLabel.endsWith("h ago"));
    }

    @Test
    public void recentOutputDataOutAgeRendersAsTheActualRecentAge() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long realisticNowMillis = 1_750_000_000_000L;
        store.recordOutputActivity(SESSION, realisticNowMillis);

        Assert.assertEquals("0s ago",
            store.lastOutputActivityAgeLabelTreatingNoDataAsVeryOld(SESSION, realisticNowMillis));
        Assert.assertEquals("5s ago",
            store.lastOutputActivityAgeLabelTreatingNoDataAsVeryOld(SESSION, realisticNowMillis + 5_000L));
    }

    @Test
    public void noOutputDataResolvesToNoneTierAndNoYellowDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));
    }

    @Test
    public void noOutputDefaultIsTheEarliestSystemTimeNotTheCurrentTime() {
        Assert.assertEquals(0L, SessionNewActivityStore.NO_OUTPUT_DEFAULT_TIME_MILLIS);
    }

    @Test
    public void saturatingElapsedMillisDoesNotOverflowForTheMinimumTimestamp() {
        long elapsedMillis = SessionNewActivityStore.saturatingElapsedMillis(50_000L, Long.MIN_VALUE);

        Assert.assertEquals(Long.MAX_VALUE, elapsedMillis);
    }

    @Test
    public void saturatingElapsedMillisComputesNormalDifferenceWhenNoOverflow() {
        Assert.assertEquals(5_000L,
            SessionNewActivityStore.saturatingElapsedMillis(55_000L, 50_000L));
    }
}
