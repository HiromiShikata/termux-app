package com.termux.app.browser;

public final class BrowserPullToRefreshGate {

    public static final int TOP_SCROLL_TOLERANCE_PIXELS = 3;

    public static final int DELIBERATE_PULL_TRIGGER_DISTANCE_DP = 160;

    private BrowserPullToRefreshGate() {
    }

    public static boolean canWebViewScrollUp(int webViewScrollY) {
        return webViewScrollY > TOP_SCROLL_TOLERANCE_PIXELS;
    }

    public static int resolveTriggerDistancePixels(float displayDensity) {
        float safeDensity = displayDensity > 0f ? displayDensity : 1f;
        return Math.round(DELIBERATE_PULL_TRIGGER_DISTANCE_DP * safeDensity);
    }
}
