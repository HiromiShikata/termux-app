package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPageLoadProgressStateTest {

    @Test
    public void startingLoadIsVisibleAtZero() {
        BrowserPageLoadProgressState state = BrowserPageLoadProgressState.forProgress(0);
        Assert.assertTrue(state.isVisible());
        Assert.assertEquals(0, state.getProgress());
    }

    @Test
    public void midwayLoadIsVisibleWithReportedProgress() {
        BrowserPageLoadProgressState state = BrowserPageLoadProgressState.forProgress(42);
        Assert.assertTrue(state.isVisible());
        Assert.assertEquals(42, state.getProgress());
    }

    @Test
    public void completedLoadIsHiddenAtFull() {
        BrowserPageLoadProgressState state = BrowserPageLoadProgressState.forProgress(100);
        Assert.assertFalse(state.isVisible());
        Assert.assertEquals(100, state.getProgress());
    }

    @Test
    public void progressBelowZeroIsClampedToZeroAndVisible() {
        BrowserPageLoadProgressState state = BrowserPageLoadProgressState.forProgress(-10);
        Assert.assertTrue(state.isVisible());
        Assert.assertEquals(0, state.getProgress());
    }

    @Test
    public void progressAboveFullIsClampedToFullAndHidden() {
        BrowserPageLoadProgressState state = BrowserPageLoadProgressState.forProgress(150);
        Assert.assertFalse(state.isVisible());
        Assert.assertEquals(100, state.getProgress());
    }
}
