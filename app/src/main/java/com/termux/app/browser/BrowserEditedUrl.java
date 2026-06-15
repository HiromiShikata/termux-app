package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserEditedUrl {

    private BrowserEditedUrl() {
    }

    @Nullable
    public static String trimmedOrNull(@Nullable String editedUrl) {
        if (editedUrl == null) return null;
        String trimmed = editedUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    public static String sessionNameFor(@Nullable String editedUrl) {
        return trimmedOrNull(editedUrl);
    }
}
