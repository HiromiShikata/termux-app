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

    public void attach(@NonNull WebView webView) {
        mScrollYByWebView.put(webView, TOP_SCROLL_Y);
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

    public void resetToTop(@NonNull WebView webView) {
        if (mScrollYByWebView.containsKey(webView)) {
            mScrollYByWebView.put(webView, TOP_SCROLL_Y);
        }
    }

    public void forget(@NonNull WebView webView) {
        mScrollYByWebView.remove(webView);
    }

    public boolean isAtTop(@Nullable WebView webView) {
        if (webView == null) {
            return false;
        }
        Integer scrollY = mScrollYByWebView.get(webView);
        return scrollY != null && scrollY <= TOP_SCROLL_Y;
    }
}
