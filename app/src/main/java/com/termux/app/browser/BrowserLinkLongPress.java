package com.termux.app.browser;

import android.webkit.WebView;

import androidx.annotation.Nullable;

public final class BrowserLinkLongPress {

    private BrowserLinkLongPress() {
    }

    public static boolean isLinkHit(int hitTestType) {
        return hitTestType == WebView.HitTestResult.SRC_ANCHOR_TYPE
            || hitTestType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
    }

    public static boolean requiresHrefLookup(int hitTestType) {
        return hitTestType == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
    }

    public static boolean isOpenableLinkUrl(@Nullable String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }
}
