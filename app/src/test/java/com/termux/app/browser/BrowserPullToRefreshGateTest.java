package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPullToRefreshGateTest {

    @Test
    public void reportsCannotScrollUpWhenWebViewIsExactlyAtTheTop() {
        Assert.assertFalse(BrowserPullToRefreshGate.canWebViewScrollUp(0));
    }

    @Test
    public void reportsCannotScrollUpWithinTheTopTolerance() {
        Assert.assertFalse(
            BrowserPullToRefreshGate.canWebViewScrollUp(BrowserPullToRefreshGate.TOP_SCROLL_TOLERANCE_PIXELS));
    }

    @Test
    public void reportsCannotScrollUpForSubPixelZoomRoundingBelowTolerance() {
        Assert.assertFalse(BrowserPullToRefreshGate.canWebViewScrollUp(1));
        Assert.assertFalse(BrowserPullToRefreshGate.canWebViewScrollUp(2));
    }

    @Test
    public void reportsCanScrollUpJustAboveTheTopTolerance() {
        Assert.assertTrue(
            BrowserPullToRefreshGate.canWebViewScrollUp(BrowserPullToRefreshGate.TOP_SCROLL_TOLERANCE_PIXELS + 1));
    }

    @Test
    public void reportsCanScrollUpWhenScrolledWellIntoThePage() {
        Assert.assertTrue(BrowserPullToRefreshGate.canWebViewScrollUp(500));
    }

    @Test
    public void reportsCanScrollUpWhenZoomedAndScrolledToALargeOffset() {
        Assert.assertTrue(BrowserPullToRefreshGate.canWebViewScrollUp(12000));
    }

    @Test
    public void treatsNegativeOverscrollPositionsAsTheTopSoRefreshStaysEnabled() {
        Assert.assertFalse(BrowserPullToRefreshGate.canWebViewScrollUp(-10));
    }

    @Test
    public void keepsTheTopScrollToleranceAtThreePixels() {
        Assert.assertEquals(3, BrowserPullToRefreshGate.TOP_SCROLL_TOLERANCE_PIXELS);
    }

    @Test
    public void disablesRefreshArmingForEveryScrollOffsetAboveTheTop() {
        for (int scrollY = BrowserPullToRefreshGate.TOP_SCROLL_TOLERANCE_PIXELS + 1; scrollY <= 5000;
                scrollY += 250) {
            Assert.assertTrue(
                "refresh must stay disabled while content remains above the viewport at scrollY=" + scrollY,
                BrowserPullToRefreshGate.canWebViewScrollUp(scrollY));
        }
    }

    @Test
    public void armsRefreshOnlyWhenTheWebViewIsAtTheTop() {
        Assert.assertFalse(BrowserPullToRefreshGate.canWebViewScrollUp(0));
        Assert.assertTrue(BrowserPullToRefreshGate.canWebViewScrollUp(4));
    }

    @Test
    public void requiresADeliberatePullDistanceLargerThanTheDefaultCircleTarget() {
        Assert.assertEquals(160, BrowserPullToRefreshGate.DELIBERATE_PULL_TRIGGER_DISTANCE_DP);
    }

    @Test
    public void scalesTheTriggerDistanceByDisplayDensity() {
        Assert.assertEquals(160, BrowserPullToRefreshGate.resolveTriggerDistancePixels(1f));
        Assert.assertEquals(320, BrowserPullToRefreshGate.resolveTriggerDistancePixels(2f));
        Assert.assertEquals(480, BrowserPullToRefreshGate.resolveTriggerDistancePixels(3f));
    }

    @Test
    public void roundsTheTriggerDistanceForFractionalDensities() {
        Assert.assertEquals(240, BrowserPullToRefreshGate.resolveTriggerDistancePixels(1.5f));
    }

    @Test
    public void fallsBackToDensityIndependentPixelsWhenDensityIsNotReported() {
        Assert.assertEquals(160, BrowserPullToRefreshGate.resolveTriggerDistancePixels(0f));
        Assert.assertEquals(160, BrowserPullToRefreshGate.resolveTriggerDistancePixels(-1f));
    }
}
