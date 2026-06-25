package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;

public final class BrowserMobileWebViewConfigurator {

    private BrowserMobileWebViewConfigurator() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void apply(@NonNull WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        BrowserWebAuthentication.apply(settings);
    }
}
