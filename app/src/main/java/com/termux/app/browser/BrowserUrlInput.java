package com.termux.app.browser;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserUrlInput {

    private static final String SEARCH_URL_PREFIX = "https://duckduckgo.com/?q=";

    private BrowserUrlInput() {
    }

    @NonNull
    public static String normalize(@Nullable String input) {
        if (input == null) return BrowserTab.DEFAULT_URL;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return BrowserTab.DEFAULT_URL;
        if (trimmed.contains("://")) return trimmed;
        if (trimmed.contains(" ") || !trimmed.contains(".")) {
            return SEARCH_URL_PREFIX + Uri.encode(trimmed);
        }
        return "https://" + trimmed;
    }
}
