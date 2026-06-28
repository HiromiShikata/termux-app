package com.termux.app.browser;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserPullToRefreshGateTest {

    private static final class ScrollableWebView extends WebView {
        private boolean canScrollUp;

        ScrollableWebView(Context context) {
            super(context);
        }

        void setCanScrollUp(boolean value) {
            this.canScrollUp = value;
        }

        @Override
        public boolean canScrollVertically(int direction) {
            if (direction == BrowserPullToRefreshGate.SCROLL_UP_DIRECTION) {
                return canScrollUp;
            }
            return super.canScrollVertically(direction);
        }
    }

    private ScrollableWebView mWebView;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        mWebView = new ScrollableWebView(activity);
    }

    @Test
    public void reportsCannotScrollUpWhenTheWebViewHasNoContentAboveTheViewport() {
        mWebView.setCanScrollUp(false);

        Assert.assertFalse(BrowserPullToRefreshGate.canWebViewScrollUp(mWebView));
    }

    @Test
    public void reportsCanScrollUpWhenTheWebViewHasContentAboveTheViewport() {
        mWebView.setCanScrollUp(true);

        Assert.assertTrue(BrowserPullToRefreshGate.canWebViewScrollUp(mWebView));
    }

    @Test
    public void gatesArmingByQueryingTheUpwardScrollDirection() {
        Assert.assertEquals(-1, BrowserPullToRefreshGate.SCROLL_UP_DIRECTION);
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
