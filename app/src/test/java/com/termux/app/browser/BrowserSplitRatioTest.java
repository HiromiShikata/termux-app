package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSplitRatioTest {

    private static final float DELTA = 1e-6f;

    @Test
    public void clampsRatioBelowMinimumUpToCollapsed() {
        Assert.assertEquals(BrowserSplitRatio.MIN, BrowserSplitRatio.clamp(-0.2f), DELTA);
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
    public void keepsRatioBelowTheTwoThirdsDefaultUnchanged() {
        Assert.assertEquals(0.4f, BrowserSplitRatio.clamp(0.4f), DELTA);
    }

    @Test
    public void keepsTheTwoThirdsDefaultUnchanged() {
        Assert.assertEquals(BrowserSplitRatio.DEFAULT, BrowserSplitRatio.clamp(BrowserSplitRatio.DEFAULT), DELTA);
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
    public void minimumIsCollapsedAndDefaultIsTwoThirdsAndMaximumIsFullScreen() {
        Assert.assertEquals(0f, BrowserSplitRatio.MIN, DELTA);
        Assert.assertEquals(2f / 3f, BrowserSplitRatio.DEFAULT, DELTA);
        Assert.assertEquals(1f, BrowserSplitRatio.MAX, DELTA);
    }

    @Test
    public void reportsCollapsedWhenRatioIsAtOrBelowTheCollapseThreshold() {
        Assert.assertTrue(BrowserSplitRatio.isCollapsed(BrowserSplitRatio.MIN));
        Assert.assertTrue(BrowserSplitRatio.isCollapsed(BrowserSplitRatio.COLLAPSE_THRESHOLD));
        Assert.assertTrue(BrowserSplitRatio.isCollapsed(BrowserSplitRatio.COLLAPSE_THRESHOLD / 2f));
    }

    @Test
    public void reportsCollapsedWhenRatioIsNegativeBecauseItClampsToTheMinimum() {
        Assert.assertTrue(BrowserSplitRatio.isCollapsed(-1f));
    }

    @Test
    public void reportsNotCollapsedAtTheTwoThirdsDefaultAndAtFullScreen() {
        Assert.assertFalse(BrowserSplitRatio.isCollapsed(BrowserSplitRatio.DEFAULT));
        Assert.assertFalse(BrowserSplitRatio.isCollapsed(BrowserSplitRatio.MAX));
    }

    @Test
    public void reportsNotCollapsedJustAboveTheCollapseThreshold() {
        Assert.assertFalse(BrowserSplitRatio.isCollapsed(BrowserSplitRatio.COLLAPSE_THRESHOLD + 0.05f));
    }
}
