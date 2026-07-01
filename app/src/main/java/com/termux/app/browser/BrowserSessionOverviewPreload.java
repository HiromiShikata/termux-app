package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserSessionOverviewPreload {

    private static final String URL_SCHEME_SEPARATOR = "://";

    @Nullable
    public static String resolvePreloadUrl(@Nullable String sessionName,
                                           @NonNull BrowserProjectActionUrls actionUrls) {
        if (isUrl(sessionName)) {
            return null;
        }
        return actionUrls.getOverviewUrl();
    }

    private static boolean isUrl(@Nullable String sessionName) {
        return sessionName != null && sessionName.contains(URL_SCHEME_SEPARATOR);
    }

    private BrowserSessionOverviewPreload() {
    }
}
