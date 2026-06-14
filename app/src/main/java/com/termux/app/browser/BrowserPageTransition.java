package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserPageTransition {

    private BrowserPageTransition() {
    }

    public static boolean requiresCoverWhileLoading(
        @Nullable String currentlyLoadedUrl, @NonNull String targetUrl, boolean browserWillBeVisible) {
        if (!browserWillBeVisible) return false;
        return !targetUrl.equals(currentlyLoadedUrl);
    }
}
