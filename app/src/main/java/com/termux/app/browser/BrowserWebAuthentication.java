package com.termux.app.browser;

import android.webkit.WebSettings;

import androidx.annotation.NonNull;

public final class BrowserWebAuthentication {

    private BrowserWebAuthentication() {
    }

    public static boolean shouldEnableForBrowser() {
        return false;
    }

    public static void apply(@NonNull WebSettings settings) {
    }
}
