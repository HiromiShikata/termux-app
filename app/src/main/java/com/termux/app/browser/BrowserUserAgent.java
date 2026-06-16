package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserUserAgent {

    public static final String DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final String WEB_VIEW_MARKER_PATTERN = "\\s*;\\s*\\bwv\\b|\\s+\\bwv\\b";

    private BrowserUserAgent() {
    }

    @Nullable
    public static String resolve(boolean desktopMode, @Nullable String defaultUserAgent) {
        return desktopMode ? DESKTOP_USER_AGENT : defaultUserAgent;
    }

    @Nullable
    public static String normalizeDefault(@Nullable String defaultUserAgent) {
        if (defaultUserAgent == null) {
            return null;
        }
        return defaultUserAgent.replaceFirst(WEB_VIEW_MARKER_PATTERN, "");
    }
}
