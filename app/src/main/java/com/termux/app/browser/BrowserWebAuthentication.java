package com.termux.app.browser;

import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

public final class BrowserWebAuthentication {

    public static final String REQUIRED_FEATURE = WebViewFeature.WEB_AUTHENTICATION;

    public static final int SUPPORT_LEVEL = WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER;

    private BrowserWebAuthentication() {
    }

    public static boolean shouldEnableForBrowser(boolean featureSupported) {
        return featureSupported;
    }

    public static void apply(@NonNull WebSettings settings) {
        if (shouldEnableForBrowser(WebViewFeature.isFeatureSupported(REQUIRED_FEATURE))) {
            WebSettingsCompat.setWebAuthenticationSupport(settings, SUPPORT_LEVEL);
        }
    }
}
