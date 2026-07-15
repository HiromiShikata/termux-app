package com.termux.app.browser;

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.termux.shared.logger.Logger;

import java.util.Collections;
import java.util.Set;

public final class BrowserPasskeyDetectionInjector {

    private static final String LOG_TAG = "BrowserPasskeyDetectionInjector";

    public static final Set<String> ALLOWED_ORIGIN_RULES = Collections.singleton("*");

    private BrowserPasskeyDetectionInjector() {
    }

    public static boolean supportsDocumentStartInjection() {
        return WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
    }

    public static void applyDocumentStart(@NonNull WebView webView) {
        if (!supportsDocumentStartInjection()) return;
        try {
            WebViewCompat.addDocumentStartJavaScript(
                webView, BrowserPasskeyDetectionScript.documentStartScript(), ALLOWED_ORIGIN_RULES);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to add document-start passkey shim", e);
        }
    }

    public static void injectAtPageStartFallback(@NonNull WebView webView, @Nullable String url) {
        if (supportsDocumentStartInjection()) return;
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) return;
        try {
            webView.evaluateJavascript(BrowserPasskeyDetectionScript.documentStartScript(), null);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to inject passkey shim", e);
        }
    }
}
