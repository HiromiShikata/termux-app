package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserWebViewConfigurator {

    private BrowserWebViewConfigurator() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void apply(
            @NonNull WebView webView,
            @NonNull BrowserViewMode viewMode,
            @Nullable String engineUserAgent) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(viewMode.isDesktop());
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        applyDesktopUserAgent(settings, viewMode, engineUserAgent);
        BrowserWebAuthentication.apply(webView);
        BrowserRequestedWithHeader.apply(settings);
    }

    private static void applyDesktopUserAgent(
            @NonNull WebSettings settings,
            @NonNull BrowserViewMode viewMode,
            @Nullable String engineUserAgent) {
        if (!viewMode.isDesktop()) {
            return;
        }
        String desktopUserAgent = BrowserUserAgent.desktopUserAgentFrom(engineUserAgent);
        if (desktopUserAgent == null) {
            return;
        }
        settings.setUserAgentString(desktopUserAgent);
    }
}
