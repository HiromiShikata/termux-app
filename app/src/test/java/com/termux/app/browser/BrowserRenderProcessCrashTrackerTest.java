package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BrowserRenderProcessCrashTrackerTest {

    private BrowserRenderProcessCrashTracker mTracker;

    @Before
    public void setUp() {
        mTracker = new BrowserRenderProcessCrashTracker();
    }

    @Test
    public void doesNotReportLoopingForTheFirstTwoCrashesWithinTheWindow() {
        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tab", 0L));
        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tab", 1_000L));
    }

    @Test
    public void reportsLoopingOnTheThirdCrashWithinTheWindow() {
        mTracker.recordCrashAndCheckLooping("tab", 0L);
        mTracker.recordCrashAndCheckLooping("tab", 1_000L);

        Assert.assertTrue(mTracker.recordCrashAndCheckLooping("tab", 2_000L));
    }

    @Test
    public void doesNotReportLoopingWhenCrashesAreSpacedBeyondTheWindow() {
        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tab", 0L));
        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tab",
            BrowserRenderProcessCrashTracker.CRASH_WINDOW_MILLIS + 1L));
        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tab",
            2L * BrowserRenderProcessCrashTracker.CRASH_WINDOW_MILLIS + 2L));
    }

    @Test
    public void evictsCrashesOlderThanTheWindowSoOldCrashesDoNotTriggerLooping() {
        mTracker.recordCrashAndCheckLooping("tab", 0L);
        mTracker.recordCrashAndCheckLooping("tab", 1_000L);

        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tab",
            BrowserRenderProcessCrashTracker.CRASH_WINDOW_MILLIS + 2_000L));
    }

    @Test
    public void tracksCrashCountsPerTabIndependently() {
        mTracker.recordCrashAndCheckLooping("tabOne", 0L);
        mTracker.recordCrashAndCheckLooping("tabOne", 1_000L);

        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tabTwo", 2_000L));
        Assert.assertTrue(mTracker.recordCrashAndCheckLooping("tabOne", 3_000L));
    }

    @Test
    public void forgettingATabResetsItsCrashHistory() {
        mTracker.recordCrashAndCheckLooping("tab", 0L);
        mTracker.recordCrashAndCheckLooping("tab", 1_000L);
        mTracker.forgetTab("tab");

        Assert.assertFalse(mTracker.recordCrashAndCheckLooping("tab", 2_000L));
    }

    @Test
    public void keepsTheConfiguredThresholdAndWindow() {
        Assert.assertEquals(2, BrowserRenderProcessCrashTracker.MAX_CRASHES_WITHIN_WINDOW);
        Assert.assertEquals(30_000L, BrowserRenderProcessCrashTracker.CRASH_WINDOW_MILLIS);
    }
}
