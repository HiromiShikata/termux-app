package com.termux.app.browser;

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.termux.shared.logger.Logger;

import java.util.Collections;
import java.util.Set;

public final class BrowserViewportInjector {

    private static final String LOG_TAG = "BrowserViewportInjector";

    public static final Set<String> ALLOWED_ORIGIN_RULES = Collections.singleton("*");

    private BrowserViewportInjector() {
    }

    public static boolean supportsDocumentStartInjection() {
        return WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
    }

    public static void applyDocumentStart(
            @NonNull WebView webView,
            @NonNull BrowserViewMode viewMode,
            boolean injectMobileViewport) {
        if (!supportsDocumentStartInjection()) return;
        String script = scriptFor(viewMode, injectMobileViewport);
        if (script == null) return;
        try {
            WebViewCompat.addDocumentStartJavaScript(webView, script, ALLOWED_ORIGIN_RULES);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to add document-start viewport shim", e);
        }
    }

    @Nullable
    public static String scriptFor(@NonNull BrowserViewMode viewMode, boolean injectMobileViewport) {
        if (viewMode.isDesktop()) {
            return BrowserDesktopViewport.INJECTION_SCRIPT;
        }
        if (injectMobileViewport) {
            return BrowserMobileViewport.INJECTION_SCRIPT;
        }
        return null;
    }
}
