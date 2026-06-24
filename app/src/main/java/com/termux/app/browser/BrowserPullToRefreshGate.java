package com.termux.app.browser;

public final class BrowserPullToRefreshGate {

    public static final int TOP_SCROLL_TOLERANCE_PIXELS = 3;

    private BrowserPullToRefreshGate() {
    }

    public static boolean canWebViewScrollUp(int webViewScrollY) {
        return webViewScrollY > TOP_SCROLL_TOLERANCE_PIXELS;
    }
}
