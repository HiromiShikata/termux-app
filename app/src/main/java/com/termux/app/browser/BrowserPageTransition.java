package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserPageTransition {

    private BrowserPageTransition() {
    }

    public static boolean requiresBlankBeforeLoad(
        @Nullable BrowserTab currentlyDisplayedTab, @NonNull BrowserTab targetTab) {
        return currentlyDisplayedTab != targetTab;
    }
}
