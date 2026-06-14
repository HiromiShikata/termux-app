package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserPageHeaderText {

    private static final String TITLE_URL_SEPARATOR = " — ";

    private BrowserPageHeaderText() {
    }

    @NonNull
    public static String format(@Nullable String title, @Nullable String url) {
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedUrl = url == null ? "" : url.trim();
        if (trimmedTitle.isEmpty() && trimmedUrl.isEmpty()) return "";
        if (trimmedTitle.isEmpty()) return trimmedUrl;
        if (trimmedUrl.isEmpty()) return trimmedTitle;
        if (trimmedTitle.equals(trimmedUrl)) return trimmedUrl;
        return trimmedTitle + TITLE_URL_SEPARATOR + trimmedUrl;
    }
}
