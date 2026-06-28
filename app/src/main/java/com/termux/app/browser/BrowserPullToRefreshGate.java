package com.termux.app.browser;

import android.webkit.WebView;

import androidx.annotation.NonNull;

public final class BrowserPullToRefreshGate {

    public static final int SCROLL_UP_DIRECTION = -1;

    public static final int DELIBERATE_PULL_TRIGGER_DISTANCE_DP = 160;

    private BrowserPullToRefreshGate() {
    }

    public static boolean canWebViewScrollUp(@NonNull WebView webView) {
        return webView.canScrollVertically(SCROLL_UP_DIRECTION);
    }

    public static int resolveTriggerDistancePixels(float displayDensity) {
        float safeDensity = displayDensity > 0f ? displayDensity : 1f;
        return Math.round(DELIBERATE_PULL_TRIGGER_DISTANCE_DP * safeDensity);
    }
}
