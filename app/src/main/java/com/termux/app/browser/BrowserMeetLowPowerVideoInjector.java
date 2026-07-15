package com.termux.app.browser;

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.termux.shared.logger.Logger;

import java.util.Collections;
import java.util.Set;

public final class BrowserMeetLowPowerVideoInjector {

    private static final String LOG_TAG = "BrowserMeetLowPowerVideoInjector";

    public static final Set<String> ALLOWED_ORIGIN_RULES =
        Collections.singleton("https://meet.google.com");

    private BrowserMeetLowPowerVideoInjector() {
    }

    public static boolean supportsDocumentStartInjection() {
        return WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
    }

    public static void applyDocumentStart(
            @NonNull WebView webView,
            @NonNull BrowserMeetLowPowerVideoSettings settings) {
        if (!supportsDocumentStartInjection()) return;
        try {
            WebViewCompat.addDocumentStartJavaScript(
                webView, settings.toDocumentStartScript(), ALLOWED_ORIGIN_RULES);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to add document-start script", e);
        }
    }

    public static void injectAtPageStartFallback(
            @NonNull WebView webView,
            @Nullable String url,
            @NonNull BrowserMeetLowPowerVideoSettings settings) {
        if (supportsDocumentStartInjection()) return;
        if (url == null || !url.startsWith("https://meet.google.com")) return;
        try {
            webView.evaluateJavascript(settings.toDocumentStartScript(), null);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to inject low-power script", e);
        }
    }
}
