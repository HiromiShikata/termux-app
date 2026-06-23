package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionOutputProgressTrackerTest {

    @Test
    public void firstObservationWithGenuineOutputReportsNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();

        Assert.assertTrue(tracker.hasNewOutput("session-one", 3L));
    }

    @Test
    public void firstObservationWithoutAnyScrolledLinesReportsNoNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();

        Assert.assertFalse(tracker.hasNewOutput("session-one", 0L));
    }

    @Test
    public void repeatedRepaintWithoutAdvancingScrolledLinesReportsNoNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        tracker.hasNewOutput("session-one", 5L);

        Assert.assertFalse(tracker.hasNewOutput("session-one", 5L));
        Assert.assertFalse(tracker.hasNewOutput("session-one", 5L));
        Assert.assertFalse(tracker.hasNewOutput("session-one", 5L));
    }

    @Test
    public void advancingScrolledLinesReportsNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        tracker.hasNewOutput("session-one", 5L);

        Assert.assertTrue(tracker.hasNewOutput("session-one", 6L));
    }

    @Test
    public void everySessionIsTrackedIndependently() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        tracker.hasNewOutput("session-one", 4L);
        tracker.hasNewOutput("session-two", 9L);

        Assert.assertFalse(tracker.hasNewOutput("session-one", 4L));
        Assert.assertTrue(tracker.hasNewOutput("session-two", 10L));
    }

    @Test
    public void forgettingSessionResetsItsProgressBaseline() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        tracker.hasNewOutput("session-one", 7L);

        tracker.forget("session-one");

        Assert.assertTrue(tracker.hasNewOutput("session-one", 7L));
    }

    @Test
    public void repeatedRefreshTicksAtTheSameVersionNeverReportNewOutput() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        tracker.hasNewOutput("worker", 42L);

        for (int refreshTick = 0; refreshTick < 120; refreshTick++) {
            Assert.assertFalse(tracker.hasNewOutput("worker", 42L));
        }
    }
}
