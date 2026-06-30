package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserGoogleSignInRedirect {

    private BrowserGoogleSignInRedirect() {
    }

    public static boolean requiresExternalBrowser(@Nullable String url) {
        return false;
    }
}
