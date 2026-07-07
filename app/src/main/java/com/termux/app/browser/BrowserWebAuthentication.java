package com.termux.app.browser;

import android.content.Context;
import android.content.pm.PackageManager;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

public final class BrowserWebAuthentication {

    public static final String REQUIRED_FEATURE = WebViewFeature.WEB_AUTHENTICATION;

    public static final String REQUIRED_PERMISSION = "android.permission.CREDENTIAL_MANAGER_SET_ORIGIN";

    public static final int SUPPORT_LEVEL = WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER;

    private BrowserWebAuthentication() {
    }

    public static boolean shouldEnableForBrowser(boolean featureSupported, boolean permissionGranted) {
        return featureSupported && permissionGranted;
    }

    public static void apply(@NonNull WebView webView) {
        boolean featureSupported = WebViewFeature.isFeatureSupported(REQUIRED_FEATURE);
        boolean permissionGranted = hasBrowserOriginPermission(webView.getContext());
        if (shouldEnableForBrowser(featureSupported, permissionGranted)) {
            WebSettingsCompat.setWebAuthenticationSupport(webView.getSettings(), SUPPORT_LEVEL);
        }
    }

    private static boolean hasBrowserOriginPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, REQUIRED_PERMISSION)
            == PackageManager.PERMISSION_GRANTED;
    }
}
