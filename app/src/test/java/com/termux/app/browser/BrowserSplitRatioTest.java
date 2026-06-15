package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSplitRatioTest {

    private static final float DELTA = 1e-6f;

    @Test
    public void clampsRatioBelowMinimumUpToTheTwoThirdsDefault() {
        Assert.assertEquals(BrowserSplitRatio.MIN, BrowserSplitRatio.clamp(0.2f), DELTA);
    }

    @Test
    public void clampsNegativeRatioUpToTheMinimum() {
        Assert.assertEquals(BrowserSplitRatio.MIN, BrowserSplitRatio.clamp(-1f), DELTA);
    }

    @Test
    public void clampsRatioAboveMaximumDownToFullScreen() {
        Assert.assertEquals(BrowserSplitRatio.MAX, BrowserSplitRatio.clamp(1.5f), DELTA);
    }

    @Test
    public void keepsRatioWithinTheAllowedRangeUnchanged() {
        Assert.assertEquals(0.8f, BrowserSplitRatio.clamp(0.8f), DELTA);
    }

    @Test
    public void keepsTheMinimumBoundaryUnchanged() {
        Assert.assertEquals(BrowserSplitRatio.MIN, BrowserSplitRatio.clamp(BrowserSplitRatio.MIN), DELTA);
    }

    @Test
    public void keepsTheMaximumBoundaryUnchanged() {
        Assert.assertEquals(BrowserSplitRatio.MAX, BrowserSplitRatio.clamp(BrowserSplitRatio.MAX), DELTA);
    }

    @Test
    public void minimumIsTwoThirdsAndMaximumIsFullScreen() {
        Assert.assertEquals(2f / 3f, BrowserSplitRatio.MIN, DELTA);
        Assert.assertEquals(1f, BrowserSplitRatio.MAX, DELTA);
    }
}
