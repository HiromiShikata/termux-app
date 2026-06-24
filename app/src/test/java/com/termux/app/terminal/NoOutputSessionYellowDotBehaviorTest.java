package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class NoOutputSessionYellowDotBehaviorTest {

    private static final String SESSION = "no-output-session";

    @Test
    public void firstObservationSeedsTheBaselineAndIsNotTreatedAsNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();

        long visibleContentVersionAfterPrompt = 5L;
        Assert.assertFalse(tracker.hasNewOutput(SESSION, visibleContentVersionAfterPrompt));

        long unchangedVisibleContentVersion = visibleContentVersionAfterPrompt;
        for (int repaint = 0; repaint < 100; repaint++) {
            Assert.assertFalse(tracker.hasNewOutput(SESSION, unchangedVisibleContentVersion));
        }
    }

    @Test
    public void genuineVisibleOutputAfterTheSeededBaselineIsTreatedAsNewOutput() {
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
    public void noOutputDataOutAgeRendersADashRatherThanAnAge() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long realisticNowMillis = 1_750_000_000_000L;

        Assert.assertNull(store.lastOutputActivityAgeLabel(SESSION, realisticNowMillis));
    }

    @Test
    public void recordedOutputDataOutAgeRendersAsTheActualAge() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long realisticNowMillis = 1_750_000_000_000L;
        store.recordOutputActivity(SESSION, realisticNowMillis);

        Assert.assertEquals("0s ago",
            store.lastOutputActivityAgeLabel(SESSION, realisticNowMillis));
        Assert.assertEquals("5s ago",
            store.lastOutputActivityAgeLabel(SESSION, realisticNowMillis + 5_000L));
    }

    @Test
    public void noOutputDataResolvesToNoneTierAndNoYellowDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));
    }
}
