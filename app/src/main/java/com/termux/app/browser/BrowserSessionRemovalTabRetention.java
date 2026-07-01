package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserSessionRemovalTabRetention {

    private BrowserSessionRemovalTabRetention() {
    }

    public static boolean shouldDeletePersistedTabs(@NonNull BrowserSessionRemovalReason reason) {
        return reason == BrowserSessionRemovalReason.USER_CLOSE;
    }
}
