package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserSessionRemovalVisibilityRetention {

    private BrowserSessionRemovalVisibilityRetention() {
    }

    public static boolean shouldClearBrowserOpenSessionName(@NonNull BrowserSessionRemovalReason reason) {
        return reason == BrowserSessionRemovalReason.USER_CLOSE;
    }
}
