package com.termux.app.browser;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class BrowserSessionVisibilityState {

    private final Set<String> mBrowserVisibleSessionHandles = new HashSet<>();

    public void setBrowserVisible(@Nullable String sessionHandle, boolean browserVisible) {
        if (sessionHandle == null) return;
        if (browserVisible) {
            mBrowserVisibleSessionHandles.add(sessionHandle);
        } else {
            mBrowserVisibleSessionHandles.remove(sessionHandle);
        }
    }

    public boolean wasBrowserVisible(@Nullable String sessionHandle) {
        return sessionHandle != null && mBrowserVisibleSessionHandles.contains(sessionHandle);
    }

    public void clearSession(@Nullable String sessionHandle) {
        if (sessionHandle == null) return;
        mBrowserVisibleSessionHandles.remove(sessionHandle);
    }

    public boolean shouldRestoreBrowserOnSessionChange(
        @Nullable String targetSessionHandle, boolean targetSessionHasActiveTab) {
        return targetSessionHasActiveTab && wasBrowserVisible(targetSessionHandle);
    }
}
