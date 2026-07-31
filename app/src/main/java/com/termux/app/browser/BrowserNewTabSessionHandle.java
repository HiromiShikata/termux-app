package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserNewTabSessionHandle {

    private BrowserNewTabSessionHandle() {
    }

    @Nullable
    public static String resolve(@Nullable String selectedSessionHandle,
                                 @Nullable String displayedTerminalSessionHandle) {
        if (selectedSessionHandle != null) return selectedSessionHandle;
        return displayedTerminalSessionHandle;
    }
}
