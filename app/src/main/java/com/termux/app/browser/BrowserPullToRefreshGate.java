package com.termux.app.browser;

public final class BrowserPullToRefreshGate {

    public static final int DELIBERATE_PULL_TRIGGER_DISTANCE_DP = 160;

    private BrowserPullToRefreshGate() {
    }

    public static int resolveTriggerDistancePixels(float displayDensity) {
        float safeDensity = displayDensity > 0f ? displayDensity : 1f;
        return Math.round(DELIBERATE_PULL_TRIGGER_DISTANCE_DP * safeDensity);
    }
}
