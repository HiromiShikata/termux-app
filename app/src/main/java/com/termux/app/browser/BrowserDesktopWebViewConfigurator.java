package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;

public final class BrowserDesktopWebViewConfigurator {

    private BrowserDesktopWebViewConfigurator() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void apply(@NonNull WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUserAgentString(BrowserUserAgent.DESKTOP_USER_AGENT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        BrowserWebAuthentication.apply(settings);
    }
}
