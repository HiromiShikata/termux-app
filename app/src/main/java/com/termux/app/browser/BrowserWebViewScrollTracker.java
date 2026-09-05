package com.termux.app.browser;

import android.os.Build;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public final class BrowserWebViewScrollTracker {

    public static final int TOP_SCROLL_Y = 0;

    private final Map<WebView, Integer> mScrollYByWebView = new IdentityHashMap<>();
    private final Map<WebView, Boolean> mInnerScrollHasContentAbove = new IdentityHashMap<>();

    public void attach(@NonNull WebView webView) {
        mScrollYByWebView.put(webView, TOP_SCROLL_Y);
        mInnerScrollHasContentAbove.put(webView, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) ->
                recordScrollY((WebView) view, scrollY));
        }
    }

    public void recordScrollY(@NonNull WebView webView, int scrollY) {
        if (mScrollYByWebView.containsKey(webView)) {
            mScrollYByWebView.put(webView, scrollY);
        }
    }

    public void recordInnerScrollHasContentAbove(@NonNull WebView webView, boolean hasContentAbove) {
        if (mInnerScrollHasContentAbove.containsKey(webView)) {
            mInnerScrollHasContentAbove.put(webView, hasContentAbove);
        }
    }

    public void resetToTop(@NonNull WebView webView) {
        if (mScrollYByWebView.containsKey(webView)) {
            mScrollYByWebView.put(webView, TOP_SCROLL_Y);
        }
        if (mInnerScrollHasContentAbove.containsKey(webView)) {
            mInnerScrollHasContentAbove.put(webView, false);
        }
    }

    public void forget(@NonNull WebView webView) {
        mScrollYByWebView.remove(webView);
        mInnerScrollHasContentAbove.remove(webView);
    }

    public boolean isAtTop(@Nullable WebView webView) {
        if (webView == null) {
            return false;
        }
        Integer scrollY = mScrollYByWebView.get(webView);
        if (scrollY == null || scrollY > TOP_SCROLL_Y) {
            return false;
        }
        Boolean innerHasContentAbove = mInnerScrollHasContentAbove.get(webView);
        return innerHasContentAbove == null || !innerHasContentAbove;
    }
}
