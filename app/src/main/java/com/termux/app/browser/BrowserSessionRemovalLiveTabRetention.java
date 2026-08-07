package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserSessionRemovalLiveTabRetention {

    private BrowserSessionRemovalLiveTabRetention() {
    }

    public static boolean shouldKeepLiveTabs(@NonNull BrowserSessionRemovalReason reason) {
        return reason == BrowserSessionRemovalReason.RECONNECT;
    }
}
