package com.termux.app.browser;

import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

public final class BrowserWebAuthentication {

    public static final String REQUIRED_FEATURE = WebViewFeature.WEB_AUTHENTICATION;

    private BrowserWebAuthentication() {
    }

    public static boolean shouldEnableForApp(boolean featureSupported) {
        return featureSupported;
    }

    public static void apply(@NonNull WebSettings settings) {
        if (shouldEnableForApp(WebViewFeature.isFeatureSupported(REQUIRED_FEATURE))) {
            WebSettingsCompat.setWebAuthenticationSupport(
                settings, WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP);
        }
    }
}
