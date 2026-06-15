package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserCurrentPageUrl {

    private BrowserCurrentPageUrl() {
    }

    @Nullable
    public static String fullUrl(@Nullable String displayedTabUrl, @Nullable String loadedUrl) {
        String tabUrl = trimToNull(displayedTabUrl);
        if (tabUrl != null) return tabUrl;
        return trimToNull(loadedUrl);
    }

    public static boolean hasCopyableUrl(@Nullable String displayedTabUrl, @Nullable String loadedUrl) {
        return fullUrl(displayedTabUrl, loadedUrl) != null;
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
