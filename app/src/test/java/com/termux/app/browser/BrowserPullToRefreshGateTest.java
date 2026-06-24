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
}
