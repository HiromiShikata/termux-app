package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserHttpAuthPrompt {

    private BrowserHttpAuthPrompt() {
    }

    @NonNull
    public static String describe(@Nullable String host, @Nullable String realm) {
        String trimmedHost = trimmedOrEmpty(host);
        String trimmedRealm = trimmedOrEmpty(realm);
        if (!trimmedHost.isEmpty() && !trimmedRealm.isEmpty()) {
            return trimmedHost + " (" + trimmedRealm + ")";
        }
        if (!trimmedHost.isEmpty()) {
            return trimmedHost;
        }
        return trimmedRealm;
    }

    @NonNull
    private static String trimmedOrEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
