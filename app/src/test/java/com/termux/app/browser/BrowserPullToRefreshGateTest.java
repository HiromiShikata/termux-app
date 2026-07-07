package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPullToRefreshGateTest {

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
