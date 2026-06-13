package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserUserAgent {

    public static final String DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

    private BrowserUserAgent() {
    }

    @Nullable
    public static String resolve(boolean desktopMode, @Nullable String defaultUserAgent) {
        return desktopMode ? DESKTOP_USER_AGENT : defaultUserAgent;
    }
}
