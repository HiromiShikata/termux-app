package com.termux.app.browser;

import androidx.annotation.Nullable;

import java.util.Objects;

public final class BrowserSessionSwitch {

    private BrowserSessionSwitch() {
    }

    public static boolean requiresTerminalOnSessionChange(
        @Nullable String currentSessionHandle, @Nullable String newSessionHandle) {
        return !Objects.equals(currentSessionHandle, newSessionHandle);
    }
}
