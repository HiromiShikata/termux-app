package com.termux.app.browser;

import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.util.Collections;
import java.util.Set;

public final class BrowserRequestedWithHeader {

    public static final String REQUIRED_FEATURE = WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST;

    public static final Set<String> SUPPRESSED_ORIGIN_ALLOW_LIST = Collections.emptySet();

    private BrowserRequestedWithHeader() {
    }

    public static boolean shouldSuppressForBrowser(boolean featureSupported) {
        return featureSupported;
    }

    public static void apply(@NonNull WebSettings settings) {
        if (shouldSuppressForBrowser(WebViewFeature.isFeatureSupported(REQUIRED_FEATURE))) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, SUPPRESSED_ORIGIN_ALLOW_LIST);
        }
    }
}
